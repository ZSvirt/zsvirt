package org.zstack.storage.volume;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfig;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.ResourceOwnerAfterChangeExtensionPoint;
import org.zstack.header.image.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;
import org.zstack.storage.encrypt.VolumeSnapshotEncryptionHelper;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.volume.*;
import org.zstack.header.volume.APIGetVolumeFormatReply.VolumeFormatReplyStruct;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.AccountManager;
import org.zstack.storage.primary.PrimaryStorageDeleteBitGC;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.host.HostStatus.Connected;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class VolumeManagerImpl extends AbstractService implements VolumeManager, ManagementNodeReadyExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint, VmStateChangedExtensionPoint, VmDetachVolumeExtensionPoint,
        VmAttachVolumeExtensionPoint, HostAfterConnectedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private VolumeDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private VolumeInPlaceEncryptor volumeInPlaceEncryptor;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    private Future<Void> volumeExpungeTask;

    private void passThrough(VolumeMessage vmsg) {
        Message msg = (Message) vmsg;
        VolumeVO vo = dbf.findByUuid(vmsg.getVolumeUuid(), VolumeVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType(msg, String.format("Cannot find volume[uuid:%s], it may have been deleted", vmsg.getVolumeUuid()));
            return;
        }

        List<VolumeFactory> l = pluginRgty.getExtensionList(VolumeFactory.class);
        if (!l.isEmpty()) {
            VolumeBase volumeBase = l.get(0).makeVolumeBase(vo);
            volumeBase.handleMessage(msg);
        } else {
            VolumeBase volumeBase = new VolumeBase(vo);
            volumeBase.handleMessage(msg);
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeMessage) {
            passThrough((VolumeMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVolumeMsg) {
            handle((CreateVolumeMsg) msg);
        } else if (msg instanceof GetVolumeTaskMsg) {
            handle((GetVolumeTaskMsg) msg);
        } else if (msg instanceof GetVolumeLocalTaskMsg) {
            handle((GetVolumeLocalTaskMsg) msg);
        } else if (msg instanceof VolumeReportPrimaryStorageCapacityUsageMsg) {
            handle((VolumeReportPrimaryStorageCapacityUsageMsg) msg);
        } else if (msg instanceof CreateDataVolumeFromVolumeTemplateMsg) {
            handle((CreateDataVolumeFromVolumeTemplateMsg) msg);
        } else if (msg instanceof CreateDataVolumeFromVolumeSnapshotMsg) {
            handle((CreateDataVolumeFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof BatchSyncManagedActiveVolumeSizeMsg) {
            handle((BatchSyncManagedActiveVolumeSizeMsg) msg);
        } else if (msg instanceof BatchSyncActiveVolumeSizeOnHostMsg) {
            handle((BatchSyncActiveVolumeSizeOnHostMsg) msg);
        } else if (msg instanceof CreateDataVolumeMsg) {
            handle((CreateDataVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private boolean getShareableCapabilityFromMsg(CreateDataVolumeFromVolumeTemplateMsg msg) {
        boolean isShareable = false;

        if (msg.getApiMsg() != null &&
            msg.getApiMsg().hasSystemTag(VolumeSystemTags.SHAREABLE.getTagFormat())) {
                isShareable = true;
        }

        if (msg.hasSystemTag(VolumeSystemTags.SHAREABLE.getTagFormat())) {
            isShareable = true;
        }

        if (isShareable && StringUtils.isNotEmpty(msg.getPrimaryStorageUuid())) {
            String psType = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.type)
                    .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                    .findValue();

            if (StringUtils.isEmpty(psType)) {
                throw new OperationFailureException(operr("get primaryStorage %s type failed", msg.getPrimaryStorageUuid()));
            }

            if (!PrimaryStorageType.getSupportFeaturesTypes(PrimaryStorageType::isSupportSharedVolume).contains(psType)) {
                throw new OperationFailureException(operr("primaryStorage type [%s] not support shared volume yet", psType));
            }
        }

        return isShareable;
    }

    private void handle(GetVolumeTaskMsg msg) {
        GetVolumeTaskReply reply = new GetVolumeTaskReply();
        Map<String, List<String>> mnIds = msg.getVolumeUuids().stream().collect(
                Collectors.groupingBy(huuid -> destMaker.makeDestination(huuid))
        );

        new While<>(mnIds.entrySet()).all((e, compl) -> {
            GetVolumeLocalTaskMsg gmsg = new GetVolumeLocalTaskMsg();
            gmsg.setVolumeUuids(e.getValue());
            bus.makeServiceIdByManagementNodeId(gmsg, VolumeConstant.SERVICE_ID, e.getKey());
            bus.send(gmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        GetVolumeLocalTaskReply gr = r.castReply();
                        reply.getResults().putAll(gr.getResults());
                    } else {
                        logger.error("get volume task fail, because " + r.getError().getDetails());
                    }

                    compl.done();
                }
            });

        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });

    }

    private void handle(GetVolumeLocalTaskMsg msg) {
        GetVolumeLocalTaskReply reply = new GetVolumeLocalTaskReply();
        List<VolumeVO> vos = Q.New(VolumeVO.class).in(VolumeVO_.uuid, msg.getVolumeUuids()).list();
        vos.forEach(vo -> reply.putResults(vo.getUuid(), thdf.getChainTaskInfo(new VolumeBase(vo).syncThreadId)));
        bus.reply(msg, reply);
    }

    private void handle(CreateDataVolumeFromVolumeTemplateMsg msg) {
        CreateDataVolumeFromVolumeTemplateReply reply = new CreateDataVolumeFromVolumeTemplateReply();

        final String originVolumeUuid = msg instanceof CreateTemporaryDataVolumeFromVolumeTemplateMsg ?
                ((CreateTemporaryDataVolumeFromVolumeTemplateMsg) msg).getOriginVolumeUuid() : null;
        final ImageVO template = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        final VolumeVO vol = new VolumeVO();
        vol.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vol.setName(msg.getName());
        vol.setDescription(msg.getDescription());
        vol.setFormat(template.getFormat());
        vol.setSize(template.getSize());
        vol.setActualSize(template.getActualSize());
        vol.setRootImageUuid(template.getUuid());
        vol.setStatus(VolumeStatus.Creating);
        vol.setState(VolumeState.Enabled);
        vol.setType(VolumeType.Data);
        vol.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        vol.setAccountUuid(msg.getAccountUuid());
        vol.setShareable(getShareableCapabilityFromMsg(msg));
        // Do not pre-mark encrypted here. The template bits we are about to download
        // are plain; if we set encrypted=true now, encryptInPlace's idempotent
        // short-circuit (volume.isEncrypted() => no-op) would skip the actual
        // qemu-img convert and we'd be left with a row claiming encryption while
        // the file is plain. encryptInPlace itself sets encrypted=true once the
        // conversion actually succeeds.
        vol.setEncrypted(false);

        if (msg.getSystemTags() != null) {
            Iterator<String> iterators = msg.getSystemTags().iterator();
            while (iterators.hasNext()) {
                String tag = iterators.next();
                if (VolumeSystemTags.VOLUME_QOS.isMatch(tag)) {
                    vol.setVolumeQos(VolumeSystemTags.VOLUME_QOS.getTokenByTag(tag, VolumeSystemTags.VOLUME_QOS_TOKEN));
                    iterators.remove();
                    break;
                }
            }
        }

        VolumeVO vvo = new SQLBatchWithReturn<VolumeVO>() {
            @Override
            protected VolumeVO scripts() {
                persist(vol);
                reload(vol);
                if (msg.getApiMsg() != null) {
                    tagMgr.createTagsFromAPICreateMessage(msg.getApiMsg(), vol.getUuid(), VolumeVO.class.getSimpleName());
                }
                return vol;
            }
        }.execute();

        if (msg.getSystemTags() != null) {
            tagMgr.createNonInherentSystemTags(msg.getSystemTags(), vvo.getUuid(), VolumeVO.class.getSimpleName());
        }

        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(null, VolumeInventory.valueOf(vvo));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-from-template-%s", template.getUuid()));
        chain.then(new ShareFlow() {
            ImageBackupStorageRefVO targetBackupStorageRef;
            PrimaryStorageInventory targetPrimaryStorage;
            String primaryStorageInstallPath;
            String prePSInstallPath;
            String volumeFormat;
            String volumeProtocol;
            String allocatedInstallUrl;
            String selectedHostUuid;
            boolean encryptInPlaceRequired;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "select-backup-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> bsUuids = new SQLBatchWithReturn<List<String>>() {
                            @Override
                            protected List<String> scripts() {
                                List<String> bsUuids = transformAndRemoveNull(template.getBackupStorageRefs(),
                                        arg -> ImageStatus.Deleted.equals(arg.getStatus()) ? null : arg.getBackupStorageUuid());

                                if (bsUuids.isEmpty()) {
                                    throw new OperationFailureException(operr("the image[uuid:%s, name:%s] has been deleted on all backup storage", template.getUuid(), template.getName()));
                                }

                                String sql = "select bs.uuid from BackupStorageVO bs, BackupStorageZoneRefVO zref, PrimaryStorageVO ps where zref.zoneUuid = ps.zoneUuid and bs.status = :bsStatus and ps.uuid = :psUuid and zref.backupStorageUuid = bs.uuid and bs.uuid in (:bsUuids)";
                                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                                q.setParameter("psUuid", msg.getPrimaryStorageUuid());
                                q.setParameter("bsStatus", BackupStorageStatus.Connected);
                                q.setParameter("bsUuids", bsUuids);
                                bsUuids = q.getResultList();

                                return bsUuids;
                            }
                        }.execute();


                        if (bsUuids.isEmpty()) {
                            trigger.fail(operr("cannot find a backup storage on which the image[uuid:%s] is that satisfies all conditions of: 1. has state Enabled 2. has status Connected. 3 has attached to zone in which primary storage[uuid:%s] is",
                                    template.getUuid(), msg.getPrimaryStorageUuid()));
                            return;
                        }

                        final String bsUuid = bsUuids.get(0);
                        targetBackupStorageRef = CollectionUtils.findOneOrNull(template.getBackupStorageRefs(),
                                arg -> arg.getBackupStorageUuid().equals(bsUuid));

                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setSize(template.getSize());
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
                        amsg.setRequiredPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        amsg.setRequiredHostUuid(msg.getHostUuid());
                        if (msg.getApiMsg() != null) {
                            amsg.setSystemTags(msg.getApiMsg().getSystemTags());
                        } else {
                            amsg.setSystemTags(msg.getSystemTags());
                        }

                        if (vvo.isShareable()) {
                            amsg.setRequiredFeatures(Collections.singleton(PrimaryStorageFeature.SHARED_VOLUME));
                        }

                        bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstallUrl = ar.getAllocatedInstallUrl();
                                targetPrimaryStorage = ar.getPrimaryStorageInventory();
                                encryptInPlaceRequired = requiresEncryptInPlace(msg);
                                selectedHostUuid = msg.getHostUuid();
                                String allocatedHostUuid = getHostUuidFromAllocatedInstallUrl(
                                        targetPrimaryStorage.getType(), allocatedInstallUrl);
                                if (StringUtils.isNotBlank(allocatedHostUuid)) {
                                    selectedHostUuid = allocatedHostUuid;
                                }
                                if (encryptInPlaceRequired && StringUtils.isBlank(selectedHostUuid)) {
                                    HostInventory host = selectHostForEncryptInPlace(targetPrimaryStorage.getUuid());
                                    if (host == null) {
                                        trigger.fail(operr(
                                                "cannot encrypt volume[uuid:%s] in place: no connected KVM host found for primary storage[uuid:%s]",
                                                vol.getUuid(), targetPrimaryStorage.getUuid()));
                                        return;
                                    }
                                    selectedHostUuid = host.getUuid();
                                }
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (targetPrimaryStorage != null) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setDiskSize(template.getSize());
                            rmsg.setPrimaryStorageUuid(targetPrimaryStorage.getUuid());
                            rmsg.setAllocatedInstallUrl(allocatedInstallUrl);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, targetPrimaryStorage.getUuid());
                            bus.send(rmsg);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-download-data-volume-template-to-primary-storage-for-garbage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetInstallPathForDataVolumeDownloadMsg gmsg = originVolumeUuid != null ?
                                new GetInstallPathForTemporaryDataVolumeDownloadMsg(originVolumeUuid) :
                                new GetInstallPathForDataVolumeDownloadMsg();
                        gmsg.setPrimaryStorageUuid(targetPrimaryStorage.getUuid());
                        gmsg.setVolumeUuid(vol.getUuid());
                        gmsg.setBackupStorageRef(ImageBackupStorageRefInventory.valueOf(targetBackupStorageRef));
                        gmsg.setImage(ImageInventory.valueOf(template));
                        gmsg.setHostUuid(selectedHostUuid);
                        gmsg.setAllocatedInstallUrl(allocatedInstallUrl);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, targetPrimaryStorage.getUuid());
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    GetInstallPathForDataVolumeDownloadReply r = reply.castReply();
                                    prePSInstallPath = r.getInstallPath();
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "download-data-volume-template-to-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DownloadDataVolumeToPrimaryStorageMsg dmsg = originVolumeUuid != null ?
                                new DownloadTemporaryDataVolumeToPrimaryStorageMsg(originVolumeUuid) :
                                new DownloadDataVolumeToPrimaryStorageMsg();
                        dmsg.setPrimaryStorageUuid(targetPrimaryStorage.getUuid());
                        dmsg.setVolumeUuid(vol.getUuid());
                        dmsg.setBackupStorageRef(ImageBackupStorageRefInventory.valueOf(targetBackupStorageRef));
                        dmsg.setImage(ImageInventory.valueOf(template));
                        dmsg.setHostUuid(selectedHostUuid);
                        dmsg.setAllocatedInstallUrl(allocatedInstallUrl);
                        dmsg.setEncryptedVolumeBackupRestore(msg.isEncryptedVolumeBackupRestore());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, targetPrimaryStorage.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    DownloadDataVolumeToPrimaryStorageReply r = reply.castReply();
                                    primaryStorageInstallPath = r.getInstallPath();
                                    volumeFormat = r.getFormat();
                                    volumeProtocol = r.getProtocol();
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (primaryStorageInstallPath != null) {
                            // if primaryStorageInstallPath != null, then delete it directly
                            DeleteVolumeBitsOnPrimaryStorageMsg delMsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                            delMsg.setInstallPath(primaryStorageInstallPath);
                            delMsg.setBitsUuid(vol.getUuid());
                            delMsg.setBitsType(VolumeVO.class.getSimpleName());
                            delMsg.setPrimaryStorageUuid(targetPrimaryStorage.getUuid());
                            delMsg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(vol.getFormat()).toString());
                            bus.makeTargetServiceIdByResourceUuid(delMsg, PrimaryStorageConstant.SERVICE_ID, targetPrimaryStorage.getUuid());
                            bus.send(delMsg);
                        } else if (PrimaryStorageGlobalConfig.PRIMARY_STORAGE_DELETEBITS_ON.value(Boolean.class)) {
                            // if primaryStorageInstallPath == null, we don't know the status agent running, so use garbage to delete
                            PrimaryStorageDeleteBitGC gc = new PrimaryStorageDeleteBitGC();
                            gc.NAME = String.format("gc-delete-bits-volume-%s-on-primary-storage-%s", vol.getUuid(), targetPrimaryStorage.getUuid());
                            gc.primaryStorageInstallPath = prePSInstallPath;
                            gc.primaryStorageUuid = targetPrimaryStorage.getUuid();
                            gc.volume = vol;
                            gc.submit(PrimaryStorageGlobalConfig.PRIMARY_STORAGE_DELETEBITS_GARBAGE_COLLECTOR_INTERVAL.value(Long.class),
                                    TimeUnit.SECONDS);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("encrypt data volume %s in place if needed", vol.getUuid());

                    @Override
                    public boolean skip(Map data) {
                        return !encryptInPlaceRequired;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeInPlaceEncryptor.Context ctx = new VolumeInPlaceEncryptor.Context()
                                .setHostUuid(selectedHostUuid)
                                .setPrimaryStorageUuid(targetPrimaryStorage.getUuid())
                                .setInstallPath(primaryStorageInstallPath)
                                .setPurpose("create-data-volume-from-template");
                        volumeInPlaceEncryptor.encryptInPlace(vol, ctx, new ReturnValueCompletion<VolumeVO>(trigger) {
                            @Override
                            public void success(VolumeVO latest) {
                                if (StringUtils.isNotBlank(latest.getInstallPath())) {
                                    primaryStorageInstallPath = latest.getInstallPath();
                                }
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("inherit key for encrypted data volume %s from snapshot template", vol.getUuid());

                    @Override
                    public boolean skip(Map data) {
                        return !isTemplateFromEncryptedSource(msg.getImageUuid());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeVO latest = dbf.reload(vol);
                        snapshotEncryptionHelper.inheritFromTemporarySnapshotImageKeyIfPossible(latest);
                        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vol.getUuid()).set(VolumeVO_.encrypted, true).update();
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("sync volume %s size", vol.getUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeVO vo = dbf.reload(vol);
                        if (vo == null) {
                            trigger.fail(operr("target volume is expunged during volume creation"));
                            return;
                        }
                        vo.setInstallPath(primaryStorageInstallPath);
                        vo.setStatus(VolumeStatus.Ready);
                        if (volumeFormat != null) {
                            vo.setFormat(volumeFormat);
                        }
                        if (volumeProtocol != null) {
                            vo.setProtocol(volumeProtocol);
                        }
                        dbf.updateAndRefresh(vo);

                        SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                        msg.setVolumeUuid(vol.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("sync volume %s size failed", vol.getUuid()));
                                }
                                SyncVolumeSizeReply sr = reply.castReply();
                                vol.setActualSize(sr.getActualSize());
                                vol.setSize(sr.getSize());
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeVO vo = dbf.reload(vol);
                        if (vo == null) {
                            reply.setError(operr("target volume is expunged during volume creation"));
                            bus.reply(msg, reply);
                            return;
                        }
                        vo.setActualSize(vol.getActualSize());
                        vo = dbf.updateAndRefresh(vo);

                        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(VolumeStatus.Creating, VolumeInventory.valueOf(vo));

                        reply.setInventory(VolumeInventory.valueOf(vo));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        reply.setSuccess(false);
                        dbf.removeByPrimaryKey(vol.getUuid(), vol.getClass());
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Transactional(readOnly = true)
    private void handle(VolumeReportPrimaryStorageCapacityUsageMsg msg) {
        String sql = "select sum(vol.size) from VolumeVO vol where vol.primaryStorageUuid = :prUuid and vol.status = :status";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("prUuid", msg.getPrimaryStorageUuid());
        q.setParameter("status", VolumeStatus.Ready);
        Long size = q.getSingleResult();

        VolumeReportPrimaryStorageCapacityUsageReply reply = new VolumeReportPrimaryStorageCapacityUsageReply();
        reply.setUsedCapacity(size == null ? 0 : size);
        bus.reply(msg, reply);
    }

    public static void preCheckPrimaryStorage(VolumeCreateMessage msg) {
        String diskOffering = msg.getDiskOfferingUuid();
        if (diskOffering == null || msg.getPrimaryStorageUuid() == null ||
                !DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            return;
        }

        DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, DiskOfferingUserConfig.class);
        if (config.getAllocate() == null) {
            return;
        }

        if (!config.getAllocate().getAllPrimaryStorages().isEmpty()) {
            List<String> requiredPrimaryStorageUuids = config.getAllocate().getAllPrimaryStorages().stream()
                    .map(PrimaryStorageAllocateConfig::getUuid).collect(Collectors.toList());
            if (!requiredPrimaryStorageUuids.contains(msg.getPrimaryStorageUuid())) {
                throw new OperationFailureException(operr("primary storage uuid conflict, the primary storage specified by the disk offering are %s, and the primary storage specified in the creation parameter is %s",
                        requiredPrimaryStorageUuids, msg.getPrimaryStorageUuid()));
            }
        }
    }

    private VolumeInventory createVolume(CreateVolumeMsg msg) {
        preCheckPrimaryStorage(msg);
        VolumeVO vo = new VolumeVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setRootImageUuid(msg.getRootImageUuid());
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setProtocol(msg.getProtocol());
        vo.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        vo.setSize(msg.getSize());
        vo.setActualSize(msg.getActualSize());
        vo.setVmInstanceUuid(msg.getVmInstanceUuid());
        vo.setFormat(msg.getFormat());
        vo.setStatus(VolumeStatus.NotInstantiated);
        vo.setType(VolumeType.valueOf(msg.getVolumeType()));
        vo.setDiskOfferingUuid(msg.getDiskOfferingUuid());
        vo.setEncrypted(Boolean.TRUE.equals(msg.getEncrypted()) ||
                (vo.getType() == VolumeType.Root && isTemplateFromEncryptedSource(msg.getRootImageUuid())));
        if (vo.getType() == VolumeType.Root) {
            vo.setDeviceId(0);
        }
        vo.setAccountUuid(msg.getAccountUuid());
        if (msg.hasSystemTag(VolumeSystemTags.SHAREABLE.getTagFormat())) {
            vo.setShareable(true);
        }

        if (msg.getSystemTags() != null) {
            Iterator<String> iterators = msg.getSystemTags().iterator();
            while (iterators.hasNext()) {
                String tag = iterators.next();
                if (VolumeSystemTags.VOLUME_QOS.isMatch(tag)) {
                    vo.setVolumeQos(VolumeSystemTags.VOLUME_QOS.getTokenByTag(tag, VolumeSystemTags.VOLUME_QOS_TOKEN));
                    iterators.remove();
                    break;
                }
            }
        }

        List<CreateDataVolumeExtensionPoint> exts = pluginRgty.getExtensionList(CreateDataVolumeExtensionPoint.class);
        for (CreateDataVolumeExtensionPoint ext : exts) {
            ext.beforeCreateVolume(VolumeInventory.valueOf(vo));
        }

        VolumeVO finalVo = vo;
        vo = new SQLBatchWithReturn<VolumeVO>() {
            @Override
            protected VolumeVO scripts() {
                dbf.getEntityManager().persist(finalVo);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(finalVo);
                return finalVo;
            }
        }.execute();
        if (msg.getSystemTags() != null) {
            tagMgr.createNonInherentSystemTags(msg.getSystemTags(), vo.getUuid(), VolumeVO.class.getSimpleName());
        }

        for (CreateDataVolumeExtensionPoint ext : exts) {
            ext.afterCreateVolume(vo);
        }
        vo = dbf.reload(vo);

        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(null, VolumeInventory.valueOf(vo));

        VolumeInventory inv = VolumeInventory.valueOf(vo);
        logger.debug(String.format("successfully created volume[uuid:%s, name:%s, type:%s, vm uuid:%s",
                inv.getUuid(), inv.getName(), inv.getType(), inv.getVmInstanceUuid()));
        return inv;
    }

    private boolean requiresEncryptInPlace(CreateDataVolumeFromVolumeTemplateMsg msg) {
        return Boolean.TRUE.equals(msg.getEncrypted()) && !isTemplateFromEncryptedSource(msg.getImageUuid());
    }

    private String getHostUuidFromAllocatedInstallUrl(String primaryStorageType, String allocatedInstallUrl) {
        if (StringUtils.isBlank(primaryStorageType) || StringUtils.isBlank(allocatedInstallUrl)) {
            return null;
        }

        PSCapacityExtensionPoint ext = pluginRgty.getExtensionFromMap(primaryStorageType, PSCapacityExtensionPoint.class);
        return ext == null ? null : ext.getHostUuidFromAllocatedInstallUrl(allocatedInstallUrl);
    }

    private HostInventory selectHostForEncryptInPlace(String primaryStorageUuid) {
        SimpleQuery<PrimaryStorageClusterRefVO> clusterRefQuery = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        clusterRefQuery.select(PrimaryStorageClusterRefVO_.clusterUuid);
        clusterRefQuery.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, primaryStorageUuid);
        List<String> clusterUuids = clusterRefQuery.listValue();
        if (clusterUuids.isEmpty()) {
            return null;
        }

        SimpleQuery<PrimaryStorageHostRefVO> hostRefQuery = dbf.createQuery(PrimaryStorageHostRefVO.class);
        hostRefQuery.add(PrimaryStorageHostRefVO_.primaryStorageUuid, Op.EQ, primaryStorageUuid);
        List<PrimaryStorageHostRefVO> hostRefs = hostRefQuery.list();
        List<String> connectedHostUuids = hostRefs.stream()
                .filter(ref -> PrimaryStorageHostStatus.Connected == ref.getStatus())
                .map(PrimaryStorageHostRefVO::getHostUuid)
                .collect(Collectors.toList());
        if (!hostRefs.isEmpty() && connectedHostUuids.isEmpty()) {
            return null;
        }

        SimpleQuery<HostVO> hostQuery = dbf.createQuery(HostVO.class);
        hostQuery.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        if (!connectedHostUuids.isEmpty()) {
            hostQuery.add(HostVO_.uuid, Op.IN, connectedHostUuids);
        }
        hostQuery.add(HostVO_.hypervisorType, Op.EQ, VmInstanceConstant.KVM_HYPERVISOR_TYPE);
        hostQuery.add(HostVO_.status, Op.EQ, HostStatus.Connected);
        hostQuery.add(HostVO_.state, Op.EQ, HostState.Enabled);
        hostQuery.orderBy(HostVO_.uuid, SimpleQuery.Od.ASC);
        hostQuery.setLimit(1);
        HostVO host = hostQuery.find();
        return host == null ? null : HostInventory.valueOf(host);
    }

    @Override
    public ErrorableValue<String> findConnectedKvmHostByPrimaryStorage(String primaryStorageUuid, String storageDescription, String operation) {
        HostInventory host = selectHostForEncryptInPlace(primaryStorageUuid);
        if (host == null) {
            return ErrorableValue.ofErrorCode(operr(
                    "cannot find a connected KVM host attached to %s[uuid:%s] to %s",
                    storageDescription, primaryStorageUuid, operation));
        }
        return ErrorableValue.of(host.getUuid());
    }

    private boolean isTemplateFromEncryptedSource(String imageUuid) {
        if (StringUtils.isBlank(imageUuid)) {
            return false;
        }

        if (snapshotEncryptionHelper.hasTemporarySnapshotImageKey(imageUuid)) {
            return true;
        }

        String imageUrl = Q.New(ImageVO.class)
                .eq(ImageVO_.uuid, imageUuid)
                .select(ImageVO_.url)
                .findValue();
        if (StringUtils.isBlank(imageUrl)) {
            return false;
        }

        if (imageUrl.startsWith("volume://")) {
            String srcVolumeUuid = imageUrl.substring("volume://".length());
            return Boolean.TRUE.equals(Q.New(VolumeVO.class)
                    .eq(VolumeVO_.uuid, srcVolumeUuid)
                    .select(VolumeVO_.encrypted)
                    .findValue());
        }

        String snapshotUuid;
        if (imageUrl.startsWith(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA.length());
        } else if (imageUrl.startsWith(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA.length());
        } else {
            return false;
        }
        snapshotUuid = snapshotUuid.length() >= 32 ? snapshotUuid.substring(0, 32) : snapshotUuid;
        return Boolean.TRUE.equals(Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .select(VolumeSnapshotVO_.encrypted)
                .findValue());
    }

    private void handle(CreateVolumeMsg msg) {
        VolumeInventory inv = createVolume(msg);
        CreateVolumeReply reply = new CreateVolumeReply();
        reply.setInventory(inv);
        bus.reply(msg, reply);
    }

    private void handle(APICreateDataVolumeFromVolumeSnapshotMsg msg) {
        final APICreateDataVolumeFromVolumeSnapshotEvent evt = new APICreateDataVolumeFromVolumeSnapshotEvent(msg.getId());

        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
        sq.select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid);
        sq.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getVolumeSnapshotUuid());
        Tuple t = sq.findTuple();
        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);

        final VolumeVO vo = new VolumeVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(VolumeState.Enabled);
        vo.setStatus(VolumeStatus.Creating);
        vo.setType(VolumeType.Data);
        vo.setSize(0);
        Boolean snapshotEncrypted = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getVolumeSnapshotUuid())
                .select(VolumeSnapshotVO_.encrypted)
                .findValue();
        vo.setEncrypted(Boolean.TRUE.equals(snapshotEncrypted));
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch)) {
            String rootImageUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).select(VolumeVO_.rootImageUuid).findValue();
            vo.setRootImageUuid(rootImageUuid);
        }

        VolumeVO vvo = new SQLBatchWithReturn<VolumeVO>() {
            @Override
            protected VolumeVO scripts() {
                persist(vo);
                reload(vo);
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), VolumeVO.class.getSimpleName());
                return vo;
            }
        }.execute();

        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(null, VolumeInventory.valueOf(vvo));
        for (CreateDataVolumeExtensionPoint ext : pluginRgty.getExtensionList(CreateDataVolumeExtensionPoint.class)) {
            ext.afterCreateVolume(vvo, msg.getVolumeSnapshotUuid());
        }
        vvo = dbf.reload(vvo);

        instantiateDataVolumeFromSnapshot(vo, msg.getVolumeSnapshotUuid(), msg.getSystemTags(), new ReturnValueCompletion<VolumeInventory>(evt) {
            @Override
            public void success(VolumeInventory volume) {
                evt.setInventory(volume);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(BatchSyncManagedActiveVolumeSizeMsg msg) {
        BatchSyncManagedActiveVolumeSizeReply reply = new BatchSyncManagedActiveVolumeSizeReply();

        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .eq(HostVO_.status, Connected)
                .listValues().stream()
                .filter(uuid -> destMaker.isManagedByUs((String) uuid)).map(String::valueOf)
                .collect(Collectors.toList());

       new While<>(hostUuids).step((hostUuid, completion) -> {
           BatchSyncActiveVolumeSizeOnHostMsg bmsg = new BatchSyncActiveVolumeSizeOnHostMsg();
           bmsg.setHostUuid(hostUuid);
           bus.makeLocalServiceId(bmsg, VolumeConstant.SERVICE_ID);

           bus.send(bmsg, new CloudBusCallBack(completion) {
               @Override
               public void run(MessageReply r) {
                   if (r.isSuccess()) {
                       BatchSyncActiveVolumeSizeOnHostReply br = r.castReply();
                       reply.addSuccessCount(br.getSuccessCount());
                       reply.addFailCount(br.getFailCount());
                   } else {
                       completion.addError(r.getError());
                   }
                   completion.done();
               }
           });
       }, VolumeGlobalConfig.HOST_COUNT_PER_BATCH_REFRESH_VOLUME_SIZE.value(Integer.class)).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    reply.setError(errorCodeList.getCauses().get(0));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(BatchSyncActiveVolumeSizeOnHostMsg msg) {
        BatchSyncActiveVolumeSizeOnHostReply reply = new BatchSyncActiveVolumeSizeOnHostReply();

        List<String> activeVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, msg.getHostUuid())
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .listValues();
        if (activeVmUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }
        Map<String, Map<String, String>> activeVolumesInPs = Q.New(VolumeVO.class)
                .select(VolumeVO_.primaryStorageUuid, VolumeVO_.uuid, VolumeVO_.installPath)
                .in(VolumeVO_.vmInstanceUuid, activeVmUuids).listTuple().stream()
                .collect(Collectors.groupingBy(t -> t.get(0, String.class), Collectors.toMap(
                        t -> ((Tuple)t).get(1, String.class), t -> ((Tuple)t).get(2, String.class))));

        new While<>(activeVolumesInPs.entrySet()).each((e, completion) -> {
            BatchSyncVolumeSizeOnPrimaryStorageMsg bmsg = new BatchSyncVolumeSizeOnPrimaryStorageMsg();
            bmsg.setHostUuid(msg.getHostUuid());
            bmsg.setPrimaryStorageUuid(e.getKey());
            bmsg.setVolumeUuidInstallPaths(e.getValue());
            bus.makeTargetServiceIdByResourceUuid(bmsg, PrimaryStorageConstant.SERVICE_ID, e.getKey());
            bus.send(bmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        BatchSyncVolumeSizeOnPrimaryStorageReply br = r.castReply();
                        Map<String, Long> actualSizes = br.getActualSizes();

                        reply.addSuccessCount(actualSizes.size());
                        reply.addFailCount(e.getValue().size() - actualSizes.size());

                        refreshVolume(actualSizes);
                    } else {
                        reply.addFailCount(e.getValue().size());
                    }

                    completion.done();
                }

                @Transactional(readOnly = true)
                private Map<String, Long> calculateSnapshotSize(Collection<String> volumeUuids) {
                    String sql = "select sp.uuid, sum(sp.size) from VolumeSnapshotVO sp where sp.volumeUuid in :uuids group by sp.uuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", volumeUuids);
                    List<Tuple> results = q.getResultList();
                    return results.stream().collect(Collectors.toMap(
                            tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, Long.class)));
                }

                @Transactional
                private void refreshVolume(Map<String, Long> actualSizes) {
                    actualSizes.entrySet().removeIf(actualSize -> actualSize.getValue() == null);
                    Map<String, Long> uuidSnapshotSizes = calculateSnapshotSize(actualSizes.keySet());

                    for (Map.Entry<String, Long> entry : actualSizes.entrySet()) {
                        // the actual size = volume actual size + all snapshot size
                        long volActualSize = entry.getValue() + uuidSnapshotSizes.getOrDefault(entry.getKey(), 0L);
                        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, entry.getKey())
                                .set(VolumeVO_.actualSize, volActualSize)
                                .update();
                    }
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    reply.setError(errorCodeList.getCauses().get(0));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateDataVolumeFromVolumeSnapshotMsg msg) {
        final CreateDataVolumeFromVolumeSnapshotReply reply = new CreateDataVolumeFromVolumeSnapshotReply();
        final VolumeVO vo = new VolumeVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(VolumeState.Enabled);
        vo.setStatus(VolumeStatus.Creating);
        vo.setType(VolumeType.Data);
        vo.setSize(msg.getSize() != null ? msg.getSize() : 0);
        Boolean snapshotEncrypted = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, msg.getVolumeSnapshotUuid())
                .select(VolumeSnapshotVO_.encrypted)
                .findValue();
        vo.setEncrypted(Boolean.TRUE.equals(msg.getEncrypted()) || Boolean.TRUE.equals(snapshotEncrypted));
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        VolumeVO vvo = new SQLBatchWithReturn<VolumeVO>() {
            @Override
            protected VolumeVO scripts() {
                persist(vo);
                reload(vo);

                if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
                    List<String> originSysTags = sql("select tag.tag from VolumeSnapshotVO snapshot, SystemTagVO tag" +
                            " where tag.resourceUuid = snapshot.volumeUuid" +
                            " and snapshot.uuid = :snapshotUuid", String.class)
                            .param("snapshotUuid", msg.getVolumeSnapshotUuid())
                            .list();
                    msg.setSystemTags(originSysTags);
                }
                tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vo.getUuid(), VolumeVO.class.getSimpleName());
                return vo;
            }
        }.execute();

        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(null, VolumeInventory.valueOf(vvo));
        for (CreateDataVolumeExtensionPoint ext : pluginRgty.getExtensionList(CreateDataVolumeExtensionPoint.class)) {
            ext.afterCreateVolume(vvo, msg.getVolumeSnapshotUuid());
        }
        vvo = dbf.reload(vvo);

        instantiateDataVolumeFromSnapshot(vo, msg.getVolumeSnapshotUuid(), msg.getSystemTags(), new ReturnValueCompletion<VolumeInventory>(msg) {
            @Override
            public void success(VolumeInventory volume) {
                reply.setInventory(volume);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void instantiateDataVolumeFromSnapshot(VolumeVO vo, String snapshotUuid, ReturnValueCompletion<VolumeInventory> completion) {
        instantiateDataVolumeFromSnapshot(vo, snapshotUuid, null, completion);
    }

    private void instantiateDataVolumeFromSnapshot(VolumeVO vo, String snapshotUuid, List<String> systemTags, ReturnValueCompletion<VolumeInventory> completion) {
        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
        sq.select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid);
        sq.add(VolumeSnapshotVO_.uuid, Op.EQ, snapshotUuid);
        Tuple t = sq.findTuple();
        String volumeUuid = t.get(0, String.class);
        String treeUuid = t.get(1, String.class);


        InstantiateDataVolumeFromVolumeSnapshotMsg cmsg = new InstantiateDataVolumeFromVolumeSnapshotMsg();
        cmsg.setVolumeUuid(volumeUuid);
        cmsg.setTreeUuid(treeUuid);
        cmsg.setUuid(snapshotUuid);
        cmsg.setVolume(VolumeInventory.valueOf(vo));
        cmsg.setSystemTags(systemTags);
        String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                VolumeVO vvo = dbf.reload(vo);
                if (vvo == null) {
                    reply.setError(operr("target volume is expunged during volume creation"));
                }

                if (reply.isSuccess()) {
                    InstantiateDataVolumeFromVolumeSnapshotReply cr = reply.castReply();
                    VolumeInventory inv = cr.getInventory();
                    vvo.setSize(inv.getSize());
                    vvo.setActualSize(cr.getActualSize());
                    vvo.setInstallPath(inv.getInstallPath());
                    vvo.setStatus(VolumeStatus.Ready);
                    vvo.setPrimaryStorageUuid(inv.getPrimaryStorageUuid());
                    vvo.setFormat(inv.getFormat());
                    vvo.setProtocol(inv.getProtocol());
                    vvo = dbf.updateAndRefresh(vvo);

                    new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(VolumeStatus.Creating, VolumeInventory.valueOf(vvo));
                    completion.success(VolumeInventory.valueOf(vvo));
                } else {
                    vvo = dbf.reload(vo);
                    if (vvo != null) {
                        VolumeInventory inventory = VolumeInventory.valueOf(vvo);
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeJustBeforeDeleteFromDbExtensionPoint.class),
                                ext -> ext.volumeJustBeforeDeleteFromDb(inventory));
                        dbf.remove(vvo);
                    }
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateDataVolumeMsg) {
            handle((APICreateDataVolumeMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeSnapshotMsg) {
            handle((APICreateDataVolumeFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeTemplateMsg) {
            handle((APICreateDataVolumeFromVolumeTemplateMsg) msg);
        } else if (msg instanceof APIGetVolumeFormatMsg) {
            handle((APIGetVolumeFormatMsg) msg);
        } else if (msg instanceof APIBatchSyncVolumeSizeMsg) {
            handle((APIBatchSyncVolumeSizeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIBatchSyncVolumeSizeMsg msg) {
        APIBatchSyncVolumeSizeReply reply = new APIBatchSyncVolumeSizeReply();

        new While<>(destMaker.getManagementNodesInHashRing()).each((mnId, compl) -> {
            BatchSyncManagedActiveVolumeSizeMsg bmsg = new BatchSyncManagedActiveVolumeSizeMsg();
            bmsg.setClusterUuid(msg.getClusterUuid());
            bus.makeServiceIdByManagementNodeId(bmsg, VolumeConstant.SERVICE_ID, mnId);
            bus.send(bmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        BatchSyncManagedActiveVolumeSizeReply br = r.castReply();
                        reply.addSuccessCount(br.getSuccessCount());
                        reply.addFailCount(br.getFailCount());
                    } else {
                        compl.addError(r.getError());
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    reply.setError(errorCodeList.getCauses().get(0));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetVolumeFormatMsg msg) {
        List<VolumeFormatReplyStruct> structs = new ArrayList<VolumeFormatReplyStruct>();
        for (VolumeFormat format : VolumeFormat.getAllFormats()) {
            structs.add(new VolumeFormatReplyStruct(format));
        }

        APIGetVolumeFormatReply reply = new APIGetVolumeFormatReply();
        reply.setFormats(structs);
        bus.reply(msg, reply);
    }

    private void handle(final APICreateDataVolumeFromVolumeTemplateMsg msg) {
        final APICreateDataVolumeFromVolumeTemplateEvent evt = new APICreateDataVolumeFromVolumeTemplateEvent(msg.getId());
        CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg(msg);
        bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setSuccess(false);
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }
                CreateDataVolumeFromVolumeTemplateReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    evt.setSuccess(false);
                    evt.setError(reply1.getError());
                    bus.publish(evt);
                    return;
                }

                evt.setInventory(reply1.getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handle(CreateDataVolumeMsg msg) {
        CreateVolumeReply reply = new CreateVolumeReply();
        pluginRgty.getExtensionList(CreateDataVolumeExtensionPoint.class).forEach(extensionPoint -> {
            extensionPoint.preCreateVolume(msg);
        });

        VolumeVO vo = new VolumeVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setDiskOfferingUuid( msg.getDiskOfferingUuid());
        vo.setSize(msg.getDiskSize());
        vo.setActualSize(0L);
        vo.setType(VolumeType.Data);
        vo.setStatus(VolumeStatus.NotInstantiated);
        vo.setAccountUuid(msg.getAccountUuid());
        vo.setEncrypted(Boolean.TRUE.equals(msg.getEncrypted()));

        if (msg.getSystemTags() != null) {
            Iterator<String> iterators = msg.getSystemTags().iterator();
            while (iterators.hasNext()) {
                String tag = iterators.next();
                if (VolumeSystemTags.VOLUME_QOS.isMatch(tag)) {
                    vo.setVolumeQos(VolumeSystemTags.VOLUME_QOS.getTokenByTag(tag, VolumeSystemTags.VOLUME_QOS_TOKEN));
                    iterators.remove();
                    break;
                }
            }
        }

        if (msg.hasSystemTag(VolumeSystemTags.SHAREABLE.getTagFormat())) {
            vo.setShareable(true);
        }
        List<CreateDataVolumeExtensionPoint> exts = pluginRgty.getExtensionList(CreateDataVolumeExtensionPoint.class);
        for (CreateDataVolumeExtensionPoint ext : exts) {
            ext.beforeCreateVolume(VolumeInventory.valueOf(vo));
        }

        VolumeVO finalVo1 = vo;
        vo = new SQLBatchWithReturn<VolumeVO>() {
            @Override
            protected VolumeVO scripts() {
                dbf.getEntityManager().persist(finalVo1);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(finalVo1);
                return finalVo1;
            }
        }.execute();

        if (msg.getApiMsg() != null) {
            tagMgr.createTagsFromAPICreateMessage(msg.getApiMsg(), finalVo1.getUuid(), VolumeVO.class.getSimpleName());
        }

        for (CreateDataVolumeExtensionPoint ext : exts) {
            ext.afterCreateVolume(vo);
        }
        dbf.reload(vo);

        List<String> requiredPrimaryStorageUuids = null;
        if (msg.getDiskOfferingUuid() != null && DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(msg.getDiskOfferingUuid())) {
            DiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(msg.getDiskOfferingUuid(), DiskOfferingUserConfig.class);
            if (config.getAllocate() != null && !config.getAllocate().getAllPrimaryStorages().isEmpty()) {
                requiredPrimaryStorageUuids = config.getAllocate().getAllPrimaryStorages().stream()
                        .map(PrimaryStorageAllocateConfig::getUuid).collect(Collectors.toList());
            }
        }

        if (msg.getPrimaryStorageUuid() == null && requiredPrimaryStorageUuids == null) {
            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(null, VolumeInventory.valueOf(vo));

            VolumeInventory inv = VolumeInventory.valueOf(vo);
            reply.setInventory(inv);
            logger.debug(String.format("Successfully created data volume[name:%s, uuid:%s, size:%s]", inv.getName(), inv.getUuid(), inv.getSize()));
            bus.reply(msg, reply);
            return;
        }

        InstantiateVolumeMsg imsg = new InstantiateVolumeMsg();
        imsg.setVolumeUuid(vo.getUuid());
        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        imsg.setSystemTags(msg.getSystemTags());
        imsg.setUserTags(msg.getUserTags());
        bus.makeTargetServiceIdByResourceUuid(imsg, VolumeConstant.SERVICE_ID, vo.getUuid());
        VolumeVO finalVo = vo;
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                InstantiateVolumeReply  cr = r.castReply();
                if (!cr.isSuccess()) {
                    detachVolumeKeyProviderOnCreateFailure(finalVo);
                    dbf.remove(finalVo);
                    reply.setError(cr.getError());
                } else {
                    reply.setInventory(cr.getVolume());
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void detachVolumeKeyProviderOnCreateFailure(VolumeVO volume) {
        if (!volume.isEncrypted()) {
            return;
        }

        try {
            volumeEncryptedResourceKeyBackend.detachKeyProviderFromVolume(volume.getUuid());
        } catch (Exception e) {
            logger.warn(String.format("failed to detach key provider from volume[uuid:%s] after create failure",
                    volume.getUuid()), e);
        }
    }

    private void handle(APICreateDataVolumeMsg msg) {
        APICreateDataVolumeEvent evt = new APICreateDataVolumeEvent(msg.getId());
        CreateDataVolumeMsg cmsg = new CreateDataVolumeMsg();
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        cmsg.setDiskSize(msg.getDiskSize());
        cmsg.setName(msg.getName());
        cmsg.setResourceUuid(msg.getResourceUuid());
        cmsg.setSystemTags(msg.getSystemTags());
        cmsg.setDiskOfferingUuid(msg.getDiskOfferingUuid());
        cmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        cmsg.setDescription(msg.getDescription());
        cmsg.setEncrypted(msg.getEncrypted());
        cmsg.setApiMsg(msg);
        bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                CreateVolumeReply cr = reply.castReply();
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    evt.setInventory(cr.getInventory());
                }
                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VolumeConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpungeTask();
            }
        });

        VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpungeTask();
            }
        });

        VolumeGlobalConfig.VOLUME_DELETION_POLICY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpungeTask();
            }
        });

        pluginRgty.saveExtensionAsMap(InstantiateDataVolumeOnCreationExtensionPoint.class, new Function<Object, InstantiateDataVolumeOnCreationExtensionPoint>() {
            @Override
            public Object call(InstantiateDataVolumeOnCreationExtensionPoint arg) {
                return arg.getPrimaryStorageTypeForInstantiateDataVolumeOnCreationExtensionPoint();
            }
        });

        pluginRgty.saveExtensionAsMap(MaxDataVolumeNumberExtensionPoint.class, MaxDataVolumeNumberExtensionPoint::getHypervisorTypeForMaxDataVolumeNumberExtension);

        {
            List<VolumeFactory> exts = pluginRgty.getExtensionList(
                    VolumeFactory.class);
            if (exts.size() > 1) {
                throw new OperationFailureException(operr("there should not be more than one %s implementation.",
                        VolumeFactory.class.getSimpleName()));
            }
        }

        VolumeInventory.setAttachedJudgers(pluginRgty.getExtensionList(VolumeAttachedJudger.class));

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private synchronized void startExpungeTask() {
        if (volumeExpungeTask != null) {
            volumeExpungeTask.cancel(true);
        }

        volumeExpungeTask = thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private List<Tuple> getDeletedVolumeManagedByUs() {
                int qun = 1000;
                SimpleQuery q = dbf.createQuery(VolumeVO.class);
                q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Deleted);
                q.add(VolumeVO_.type, Op.EQ, VolumeType.Data);
                long amount = q.count();
                int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
                int start = 0;
                List<Tuple> ret = new ArrayList<Tuple>();
                for (int i = 0; i < times; i++) {
                    q = dbf.createQuery(VolumeVO.class);
                    q.select(VolumeVO_.uuid, VolumeVO_.lastOpDate);
                    q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Deleted);
                    q.add(VolumeVO_.type, Op.EQ, VolumeType.Data);
                    q.setLimit(qun);
                    q.setStart(start);
                    List<Tuple> lst = q.listTuple();
                    start += qun;
                    for (Tuple t : lst) {
                        String uuid = t.get(0, String.class);
                        if (!destMaker.isManagedByUs(uuid)) {
                            continue;
                        }

                        ret.add(t);
                    }
                }

                return ret;
            }

            @Override
            public boolean run() {
                List<Tuple> vols = getDeletedVolumeManagedByUs();
                if (vols.isEmpty()) {
                    logger.debug("[Volume Expunging Task]: no volume to expunge");
                    return false;
                }

                Timestamp current = dbf.getCurrentSqlTime();
                for (final Tuple v : vols) {
                    final String uuid = v.get(0, String.class);
                    Timestamp date = v.get(1, Timestamp.class);
                    long end = date.getTime() + TimeUnit.SECONDS.toMillis(VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.value(Long.class));
                    if (current.getTime() >= end) {

                        VolumeDeletionPolicy deletionPolicy = deletionPolicyMgr.getDeletionPolicy(uuid);
                        if (deletionPolicy == VolumeDeletionPolicy.Never) {
                            logger.debug(String.format("the deletion policy of the volume[uuid:%s] is Never, don't expunge it",
                                    uuid));
                            continue;
                        }

                        ExpungeVolumeMsg msg = new ExpungeVolumeMsg();
                        msg.setVolumeUuid(uuid);
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, uuid);
                        bus.send(msg, new CloudBusCallBack(null) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to expunge the volume[uuid:%s], %s", uuid, reply.getError()));
                                } else {
                                    logger.debug(String.format("successfully expunged the volume [uuid:%s]", uuid));
                                }
                            }
                        });

                    }
                }

                return false;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "expunging-volume-task";
            }
        });

        logger.debug(String.format("volume expunging task starts [period: %s seconds, interval: %s seconds]",
                VolumeGlobalConfig.VOLUME_EXPUNGE_PERIOD.value(Long.class),
                VolumeGlobalConfig.VOLUME_EXPUNGE_INTERVAL.value(Long.class)));
    }

    @Override
    public void managementNodeReady() {
        startExpungeTask();
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        changeVolumeOwner(ref, newOwnerUuid);
        changeVolumeSnapshotGroupOwner(ref, newOwnerUuid);
    }

    private void changeVolumeSnapshotGroupOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        SimpleQuery<VolumeSnapshotGroupVO> q = dbf.createQuery(VolumeSnapshotGroupVO.class);
        q.select(VolumeSnapshotGroupVO_.uuid);
        q.add(VolumeSnapshotGroupVO_.vmInstanceUuid, Op.EQ, ref.getResourceUuid());
        List<String> uuids = q.listValue();

        for (String uuid : uuids) {
            acntMgr.changeResourceOwner(uuid, newOwnerUuid);
        }
    }

    private void changeVolumeOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.uuid);
        q.add(VolumeVO_.vmInstanceUuid, Op.EQ, ref.getResourceUuid());
        List<String> uuids = q.listValue();

        for (String uuid : uuids) {
            acntMgr.changeResourceOwner(uuid, newOwnerUuid);
        }
    }

    @Override
    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        if (newState == VmInstanceState.Destroyed && vm != null && vm.getRootVolumeUuid() != null) {
            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid())
                    .set(VolumeVO_.status, VolumeStatus.Deleted)
                    .update();
        }
        if (oldState == VmInstanceState.VolumeMigrating && newState == VmInstanceState.Stopped && vm != null && vm.getRootVolumeUuid() != null) {
            // maybe restart mn, and we need restore from VolumeMigrating state
            SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.getRootVolumeUuid()).eq(VolumeVO_.status, VolumeStatus.Migrating)
                    .set(VolumeVO_.status, VolumeStatus.Ready)
                    .update();
        }
    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        // update Volumevo before exit message queue
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid())
                .set(VolumeVO_.vmInstanceUuid, null)
                .set(VolumeVO_.deviceId, null)
                .update();
        vidm.deleteVmResourceMetadata(volume.getUuid(), vm.getUuid());
        completion.success();
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid())
                .set(VolumeVO_.vmInstanceUuid, vm.getUuid())
                .update();
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {}

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    @Override
    public void afterInstantiateVolumeForNewCreatedVm(VmInstanceInventory vm, VolumeInventory volume) {
        updateVolumeInfo(vm, volume);
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        updateVolumeInfo(vm, volume);
    }

    private void updateVolumeInfo(VmInstanceInventory vm, VolumeInventory volume) {
        String format = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid()).select(VolumeVO_.format).findValue();
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid())
                .set(VolumeVO_.vmInstanceUuid, volume.isShareable() ? null : vm.getUuid())
                .set(VolumeVO_.format, format != null ? format :
                        VolumeFormat.getVolumeFormatByMasterHypervisorType(vm.getHypervisorType()))
                .set(VolumeVO_.lastAttachDate, Timestamp.valueOf(LocalDateTime.now()))
                .update();
    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid())
                .set(VolumeVO_.vmInstanceUuid, null)
                .update();
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        String hostUuid = host.getUuid();
        List<VolumeHostRefVO> refVOs = Q.New(VolumeHostRefVO.class).eq(VolumeHostRefVO_.hostUuid, hostUuid).list();
        if (refVOs == null || refVOs.isEmpty()) {
            return;
        }
        refVOs.forEach(refVO -> {
            AttachDataVolumeToHostMsg mmsg = new AttachDataVolumeToHostMsg();
            mmsg.setHostUuid(refVO.getHostUuid());
            mmsg.setVolumeUuid(refVO.getVolumeUuid());
            mmsg.setMountPath(refVO.getMountPath());
            mmsg.setDevice(refVO.getDevice());
            bus.makeTargetServiceIdByResourceUuid(mmsg, HostConstant.SERVICE_ID, hostUuid);
            bus.send(mmsg, new CloudBusCallBack(mmsg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to mount volume[%s] on host [%s]", refVO.getVolumeUuid(), hostUuid));
                    }
                }
            });
        });
    }
}
