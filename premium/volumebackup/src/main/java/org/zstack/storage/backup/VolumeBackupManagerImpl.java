package org.zstack.storage.backup;

import com.mysql.cj.util.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.NewVmInstanceMsgBuilder;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.image.*;
import org.zstack.header.imagestore.*;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.GetPrimaryStorageResourceLocationMsg;
import org.zstack.header.storage.primary.GetPrimaryStorageResourceLocationReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.volume.backup.*;
import org.zstack.header.tag.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.identity.rbac.AccessibleResourceChecker;
import org.zstack.image.ImageQuotaConstant;
import org.zstack.image.ImageSystemTags;
import org.zstack.kvm.tpm.SnapshotGroupRevertTpmHelper;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.scheduler.SchedulerJob;
import org.zstack.scheduler.SchedulerJobParamCascadeUpdater;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.storage.backup.VolumeBackupUtils.*;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

public class VolumeBackupManagerImpl extends AbstractService implements
        VolumeBackupManager,
        ResourceOwnerAfterChangeExtensionPoint,
        VmPreMigrationExtensionPoint,
        VmBeforeStartOnHypervisorExtensionPoint,
        ImageStoreReclaimSpaceExtensionPoint,
        ReportQuotaExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeBackupManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;

    final private Map<String, VolumeBackupFactory> bkFactories = Collections.synchronizedMap(new HashMap<>());

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeBackupMessage) {
            passThrough((VolumeBackupMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SyncBackupFromImageStoreBackupStorageMsg) {
            handle((SyncBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof VolumeBackupDeletionMsg) {
            handle((VolumeBackupDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(VolumeBackupDeletionMsg msg) {
        VolumeBackupDeletionReply reply = new VolumeBackupDeletionReply();
        String volumeUuid = Q.New(VolumeBackupVO.class).select(VolumeBackupVO_.volumeUuid)
                .eq(VolumeBackupVO_.uuid, msg.getUuid())
                .findValue();
        if (volumeUuid == null) {
            bus.reply(msg, reply);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("volume-%s-backup", volumeUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (msg.isDbOnly()) {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            sql(VolumeBackupStorageRefVO.class)
                                    .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, msg.getUuid())
                                    .in(VolumeBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuids())
                                    .delete();

                            if (!q(VolumeBackupStorageRefVO.class)
                                    .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, msg.getUuid())
                                    .isExists()) {
                                sql(VolumeBackupVO.class).eq(VolumeBackupVO_.uuid, msg.getVolumeBackupUuid()).delete();
                            }
                        }
                    }.execute();

                    List<String> fileUuidList = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.resourceUuid, msg.getUuid())
                            .select(VmHostBackupFileVO_.uuid)
                            .listValues();
                    if (fileUuidList.isEmpty()) {
                        bus.reply(msg, reply);
                        chain.next();
                        return;
                    }

                    new While<>(fileUuidList).each((fileUuid, whileCompletion) -> {
                        VmHostBackupFileDeletionMsg deletionMsg = new VmHostBackupFileDeletionMsg();
                        deletionMsg.setUuid(fileUuid);
                        deletionMsg.setForceDelete(true);
                        bus.makeLocalServiceId(deletionMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                        bus.send(deletionMsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    whileCompletion.done();
                                    return;
                                }
                                whileCompletion.addError(reply.getError());
                                whileCompletion.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(msg, chain) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (!errorCodeList.isEmpty()) {
                                logger.warn("failed to delete some VmHostBackupFiles:\n" +
                                        String.join("\n",
                                                transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails)));
                            }
                            bus.reply(msg, reply);
                            chain.next();
                        }
                    });
                    return;
                }

                doDeleteVolumeBackup(msg, msg.getBackupStorageUuids(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-%s-backup-%s", volumeUuid, msg.getUuid());
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteVolumeBackupMsg) {
            handle((APIDeleteVolumeBackupMsg) msg);
        } else if (msg instanceof APIDeleteVmBackupMsg) {
            handle((APIDeleteVmBackupMsg) msg);
        } else if (msg instanceof APIRevertVolumeFromVolumeBackupMsg) {
            handle((APIRevertVolumeFromVolumeBackupMsg) msg);
        } else if (msg instanceof APIRevertVmFromVmBackupMsg) {
            handle((APIRevertVmFromVmBackupMsg) msg);
        } else if (msg instanceof APISyncBackupFromImageStoreBackupStorageMsg) {
            handle((APISyncBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISyncVmBackupFromImageStoreBackupStorageMsg) {
            handle((APISyncVmBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIRecoverBackupFromImageStoreBackupStorageMsg) {
            handle((APIRecoverBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIRecoverVmBackupFromImageStoreBackupStorageMsg) {
            handle((APIRecoverVmBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromVolumeBackupMsg) {
            handle((APICreateRootVolumeTemplateFromVolumeBackupMsg) msg);
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeBackupMsg) {
            handle((APICreateDataVolumeTemplateFromVolumeBackupMsg) msg);
        } else if (msg instanceof APICreateVmFromVmBackupMsg) {
            handle((APICreateVmFromVmBackupMsg) msg);
        } else if (msg instanceof APICreateVmFromVolumeBackupMsg) {
            handle((APICreateVmFromVolumeBackupMsg) msg);
        } else if (msg instanceof APICreateDataVolumeFromVolumeBackupMsg) {
            handle((APICreateDataVolumeFromVolumeBackupMsg) msg);
        } else if (msg instanceof APISyncVolumeBackupMsg) {
            handle((APISyncVolumeBackupMsg) msg);
        } else if (msg instanceof APISyncVmBackupMsg) {
            handle((APISyncVmBackupMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void doCreateVmFromVmBackup(APICreateVmFromVmBackupMsg msg, ReturnValueCompletion<VmInstanceInventory> returnValueCompletion) {
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("create-vm-from-vm-backup-group-uuid-%s", msg.getGroupUuid()));

        class Bundle {
            VolumeInventory newVolume;
            VolumeMetadata metadata;
            String imageUuid;
            String backupUuid;
            String originVolumeUuid;
        }

        class CreateVmFromBackupStruct {
            Bundle rootVolBucket;
            final List<Bundle> dataVolBuckets = Collections.synchronizedList(new ArrayList<>());
            String vmInstanceUuid;
        }

        final CreateVmFromBackupStruct createStruct = new CreateVmFromBackupStruct();
        chain.then(new Flow() {
            String __name__ = String.format("create-template-from-volume-backup-group-%s", msg.getGroupUuid());

            final List<String> succeedUuids = Collections.synchronizedList(new ArrayList<>());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VolumeBackupVO> vos = verifyGroupUuid(msg.getGroupUuid(), msg.getSession());
                Map<String, String> bsUuids = getBackupBsUuids(vos, msg.getBackupStorageUuid());

                new While<>(vos).step((vo, completion) -> {
                    VolumeMetadata metadata = VolumeMetaDataMaker.parseVolumeMetadata(vo.getMetadata());
                    TemplateCreateStruct struct = makeCreateTemplateStruct(vo, bsUuids.get(vo.getUuid()), msg.getSession());
                    ImageConstant.ImageMediaType mediaType = vo.getType().equals(VolumeType.Root) ? ImageConstant.ImageMediaType.RootVolumeTemplate : ImageConstant.ImageMediaType.DataVolumeTemplate;

                    doCreateTemplateFromBackup(struct, mediaType, new ReturnValueCompletion<ImageInventory>(trigger) {
                        @Override
                        public void success(ImageInventory image) {
                            Bundle bucket = new Bundle();
                            bucket.metadata = metadata;
                            bucket.imageUuid = image.getUuid();
                            bucket.backupUuid = vo.getUuid();
                            bucket.originVolumeUuid = vo.getVolumeUuid();
                            if (image.getMediaType().equals(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())) {
                                createStruct.rootVolBucket = bucket;
                            } else {
                                createStruct.dataVolBuckets.add(bucket);
                            }

                            tagMgr.createNonInherentSystemTag(image.getUuid(),
                                    ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                    ImageVO.class.getSimpleName());

                            succeedUuids.add(image.getUuid());
                            completion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            if (logger.isTraceEnabled()) {
                                logger.debug(String.format("failed to create image from volume backup %s", vo.getUuid()));
                            }

                            completion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (succeedUuids.size() != vos.size()) {
                            trigger.fail(operr("failed to create image from backup %s",
                                    vos.stream().filter(vo -> !succeedUuids.contains(vo.getUuid()))
                                            .map(VolumeBackupVO::getUuid)
                                            .collect(Collectors.toList())));
                            return;
                        }

                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (createStruct.rootVolBucket != null) {
                    deleteImageAsync(createStruct.rootVolBucket.imageUuid, null);
                }

                createStruct.dataVolBuckets.forEach(bundle -> deleteImageAsync(bundle.imageUuid, null));
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("create-vm-from-template-%s", createStruct.rootVolBucket);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                CreateVmInstanceMsg cmsg = new CreateVmInstanceMsg();
                cmsg.setSystemTags(msg.getSystemTags());
                cmsg.setZoneUuid(msg.getZoneUuid());
                cmsg.setInstanceOfferingUuid(msg.getInstanceOfferingUuid());
                cmsg.setClusterUuid(msg.getClusterUuid());
                cmsg.setName(msg.getName());
                cmsg.setImageUuid(createStruct.rootVolBucket.imageUuid);

                cmsg.setL3NetworkSpecs(NewVmInstanceMsgBuilder.getVmNicSpecsFromNewVmInstanceMsg(msg));
                cmsg.setType(msg.getType());
                cmsg.setHostUuid(msg.getHostUuid());

                if (msg.getInstanceOfferingUuid() != null) {
                    InstanceOfferingVO iovo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
                    cmsg.setInstanceOfferingUuid(iovo.getUuid());
                    cmsg.setCpuNum(iovo.getCpuNum());
                    cmsg.setCpuSpeed(iovo.getCpuSpeed());
                    cmsg.setMemorySize(iovo.getMemorySize());
                    cmsg.setAllocatorStrategy(iovo.getAllocatorStrategy());
                } else {
                    cmsg.setCpuNum(msg.getCpuNum());
                    cmsg.setMemorySize(msg.getMemorySize());
                    cmsg.setReservedMemorySize(msg.getReservedMemorySize() != null ? msg.getReservedMemorySize() : 0);
                }
                cmsg.setDescription(msg.getDescription());
                cmsg.setResourceUuid(msg.getResourceUuid());
                cmsg.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
                cmsg.setStrategy(msg.getStrategy());

                Map<String, DiskAO> diskAOBySourceUuid = Optional.ofNullable(msg.getDiskAOs())
                        .orElse(Collections.emptyList()).stream()
                        .filter(d -> d.getSourceUuid() != null)
                        .collect(Collectors.toMap(DiskAO::getSourceUuid, d -> d, (a, b) -> a));

                Bundle rootBundle = createStruct.rootVolBucket;
                DiskAO rootUserAO = diskAOBySourceUuid.get(rootBundle.originVolumeUuid);
                DiskAO rootDisk = DiskAO.rootDisk();
                rootDisk.withImage(rootBundle.imageUuid);
                rootDisk.setPrimaryStorageUuid(
                        rootUserAO != null && rootUserAO.getPrimaryStorageUuid() != null
                                ? rootUserAO.getPrimaryStorageUuid()
                                : msg.getPrimaryStorageUuidForRootVolume());
                rootDisk.setSystemTags(resolveSystemTags(rootUserAO, rootBundle.metadata, msg.getRootVolumeSystemTags()));
                if (rootUserAO != null && rootUserAO.getName() != null) {
                    rootDisk.setName(rootUserAO.getName());
                }
                cmsg.setRootDisk(rootDisk);

                // sort data disks by device-id to preserve original disk order
                createStruct.dataVolBuckets.sort(Comparator.comparing(it ->
                        it.metadata.getDeviceId() != null ? it.metadata.getDeviceId() : Integer.MAX_VALUE));

                // Data disks go through deprecatedDataVolumeSpecs for legacy template flow
                // (naming + device-id restoration). Only templateUuid/primaryStorageUuid/systemTags
                // are set; size/diskOffering omitted so allocate flows skip these.
                List<DiskAO> templateDataDisks = new ArrayList<>();
                for (Bundle dataBundle : createStruct.dataVolBuckets) {
                    DiskAO userAO = diskAOBySourceUuid.get(dataBundle.originVolumeUuid);
                    DiskAO disk = DiskAO.nonRootDisk();
                    disk.setTemplateUuid(dataBundle.imageUuid);
                    disk.setPrimaryStorageUuid(
                            userAO != null && userAO.getPrimaryStorageUuid() != null
                                    ? userAO.getPrimaryStorageUuid()
                                    : msg.getPrimaryStorageUuidForDataVolume());
                    disk.setSystemTags(resolveSystemTags(userAO, dataBundle.metadata, msg.getDataVolumeSystemTags()));
                    templateDataDisks.add(disk);
                }
                cmsg.setDeprecatedDataVolumeSpecs(templateDataDisks);

                cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                cmsg.setHeaders(msg.getHeaders());

                new SnapshotGroupRevertTpmHelper().setupFromBackupResource(
                        createStruct.rootVolBucket.backupUuid, msg.getResetTpm(), cmsg);

                bus.makeLocalServiceId(cmsg, VmInstanceConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateVmInstanceReply reply1 = reply.castReply();
                        VmInstanceInventory vm = reply1.getInventory();
                        createStruct.vmInstanceUuid = reply1.getInventory().getUuid();
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("delete-temp-images-for-vm-%s", createStruct.vmInstanceUuid);

            @Override
            public boolean skip(Map data) {
                return !MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                deleteImageAsync(createStruct.rootVolBucket.imageUuid, null);
                createStruct.dataVolBuckets.forEach(bundle -> deleteImageAsync(bundle.imageUuid, null));
                trigger.next();

            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                VmInstanceVO vm = dbf.findByUuid(createStruct.vmInstanceUuid, VmInstanceVO.class);
                returnValueCompletion.success(vm.toInventory());
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                returnValueCompletion.fail(errCode);
            }
        }).start();
    }

    private List<String> resolveSystemTags(DiskAO userAO, VolumeMetadata metadata, List<String> fallbackSystemTags) {
        if (userAO != null && userAO.getSystemTags() != null) {
            return new ArrayList<>(userAO.getSystemTags());
        }
        if (fallbackSystemTags != null) {
            return new ArrayList<>(fallbackSystemTags);
        }
        if (metadata != null && metadata.getSystemTags() != null) {
            return metadata.getSystemTags().stream()
                    .filter(it -> tagMgr.isCloneable(it.getTag(), it.getResourceType()))
                    .map(TagInventory::getTag)
                    .collect(Collectors.toList());
        }
        return null;
    }

    private void doCreateVmFromVolumeBackup(APICreateVmFromVolumeBackupMsg msg, ReturnValueCompletion<VmInstanceInventory> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            String imageUuid;
            VmInstanceInventory vmInventory;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-image-in-database";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String bsUuid = getBackupBsUuids(Collections.singletonList(msg.getVolumeBackupVO()), msg.getBackupStorageUuid()).get(msg.getVolumeBackupVO().getUuid());
                        TemplateCreateStruct struct = makeCreateTemplateStruct(msg.getVolumeBackupVO(), bsUuid, msg.getSession());
                        doCreateTemplateFromBackup(struct, ImageConstant.ImageMediaType.RootVolumeTemplate, new ReturnValueCompletion<ImageInventory>(trigger) {
                            @Override
                            public void success(ImageInventory image) {
                                imageUuid = image.getUuid();
                                tagMgr.createNonInherentSystemTag(image.getUuid(),
                                        ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                        ImageVO.class.getSimpleName());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        deleteImageAsync(imageUuid, null);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("create-vm-from-template-%s", imageUuid);

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVmInstanceMsg cmsg = new CreateVmInstanceMsg();
                        cmsg.setSystemTags(msg.getSystemTags());
                        cmsg.setZoneUuid(msg.getZoneUuid());
                        cmsg.setClusterUuid(msg.getClusterUuid());
                        cmsg.setName(msg.getName());
                        cmsg.setImageUuid(imageUuid);

                        List<L3NetworkVO> l3vos = dbf.listByPrimaryKeys(msg.getL3NetworkUuids(), L3NetworkVO.class);
                        cmsg.setL3NetworkSpecs(Collections.singletonList(new VmNicSpec(L3NetworkInventory.valueOf(l3vos))));
                        cmsg.setType(msg.getType());
                        cmsg.setHostUuid(msg.getHostUuid());
                        cmsg.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());

                        if (msg.getCpuNum() != null && msg.getMemorySize() != null) {
                            cmsg.setCpuNum(msg.getCpuNum());
                            cmsg.setMemorySize(msg.getMemorySize());
                        } else {
                            InstanceOfferingVO iovo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
                            cmsg.setInstanceOfferingUuid(iovo.getUuid());
                            cmsg.setCpuNum(iovo.getCpuNum());
                            cmsg.setCpuSpeed(iovo.getCpuSpeed());
                            cmsg.setMemorySize(iovo.getMemorySize());
                            cmsg.setAllocatorStrategy(iovo.getAllocatorStrategy());
                        }

                        cmsg.setDescription(msg.getDescription());
                        cmsg.setResourceUuid(msg.getResourceUuid());
                        cmsg.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
                        cmsg.setStrategy(msg.getStrategy());

                        VolumeMetadata metadata = VolumeMetaDataMaker.parseVolumeMetadata(msg.getVolumeBackupVO().getMetadata());

                        if (msg.getRootVolumeSystemTags() != null) {
                            cmsg.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
                        } else if (metadata != null && metadata.getSystemTags() != null) {
                            cmsg.setRootVolumeSystemTags(metadata.getSystemTags().stream()
                                    .filter(it -> tagMgr.isCloneable(it.getTag(), it.getResourceType()))
                                    .map(TagInventory::getTag)
                                    .collect(Collectors.toList()));
                        }
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        cmsg.setHeaders(msg.getHeaders());

                        new SnapshotGroupRevertTpmHelper().setupFromBackupResource(
                                msg.getBackupUuid(), msg.getResetTpm(), cmsg);

                        bus.makeLocalServiceId(cmsg, VmInstanceConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CreateVmInstanceReply r = reply.castReply();
                                vmInventory = r.getInventory();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-temp-images-for-vm";

                    @Override
                    public boolean skip(Map data) {
                        return !MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteImageAsync(imageUuid, null);
                        trigger.next();

                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        completion.success(vmInventory);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APICreateVmFromVmBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-vm-from-backups-group-%s", msg.getGroupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateVmFromVmBackupEvent evt = new APICreateVmFromVmBackupEvent(msg.getId());

                doCreateVmFromVmBackup(msg, new ReturnValueCompletion<VmInstanceInventory>(chain) {
                    @Override
                    public void success(VmInstanceInventory inv) {
                        evt.setInventory(inv);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APICreateVmFromVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-vm-from-backup-uuid-%s", msg.getBackupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateVmFromVolumeBackupEvent evt = new APICreateVmFromVolumeBackupEvent(msg.getId());

                doCreateVmFromVolumeBackup(msg, new ReturnValueCompletion<VmInstanceInventory>(chain) {
                    @Override
                    public void success(VmInstanceInventory inv) {
                        evt.setInventory(inv);
                        evt.setSuccess(true);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        evt.setSuccess(false);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APICreateDataVolumeFromVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-volume-from-backup-uuid-%s", msg.getBackupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateDataVolumeFromVolumeBackupEvent evt = new APICreateDataVolumeFromVolumeBackupEvent(msg.getId());

                doCreateDataVolumeFromVolumeBackup(msg, new ReturnValueCompletion<VolumeInventory>(chain) {
                    @Override
                    public void success(VolumeInventory inv) {
                        evt.setInventory(inv);
                        evt.setSuccess(true);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        evt.setSuccess(false);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doCreateDataVolumeFromVolumeBackup(APICreateDataVolumeFromVolumeBackupMsg msg, ReturnValueCompletion<VolumeInventory> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            String imageUuid;
            VolumeInventory volumeInventory;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-image-in-database";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String bsUuid = getBackupBsUuids(Collections.singletonList(msg.getVolumeBackupVO()), msg.getBackupStorageUuid()).get(msg.getVolumeBackupVO().getUuid());
                        TemplateCreateStruct struct = makeCreateTemplateStruct(msg.getVolumeBackupVO(), bsUuid, msg.getSession());
                        doCreateTemplateFromBackup(struct, ImageConstant.ImageMediaType.DataVolumeTemplate, new ReturnValueCompletion<ImageInventory>(trigger) {
                            @Override
                            public void success(ImageInventory image) {
                                imageUuid = image.getUuid();
                                tagMgr.createNonInherentSystemTag(image.getUuid(),
                                        ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                        ImageVO.class.getSimpleName());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        deleteImageAsync(imageUuid, null);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-data-volume-from-template";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateDataVolumeFromVolumeTemplateMsg cmsg = new CreateDataVolumeFromVolumeTemplateMsg();
                        cmsg.setName(msg.getName());
                        cmsg.setImageUuid(imageUuid);
                        cmsg.setResourceUuid(getUuid());
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        cmsg.setDescription(String.format("data-volume-volume-from-backup-%s", msg.getBackupUuid()));
                        cmsg.setSystemTags(msg.getSystemTags());
                        cmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        cmsg.setDescription(msg.getDescription());
                        if (msg.getVmInstanceUuid() != null) {
                            cmsg.setHostUuid(Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).select(VmInstanceVO_.hostUuid).findValue());
                        }

                        bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CreateDataVolumeFromVolumeTemplateReply r = reply.castReply();
                                volumeInventory = r.getInventory();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "attach-data-volume-to-vm";

                    @Override
                    public boolean skip(Map data) {
                        return msg.getVmInstanceUuid() == null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                        amsg.setVolume(volumeInventory);
                        amsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                        bus.send(amsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-temp-images-for-volume";

                    @Override
                    public boolean skip(Map data) {
                        return !MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        deleteImageAsync(imageUuid, null);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        completion.success(volumeInventory);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APISyncVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "sync-backup";
            }

            @Override
            public void run(SyncTaskChain chain) {
                APISyncVolumeBackupEvent event = new APISyncVolumeBackupEvent(msg.getId());
                SyncBackupResult result = null;
                try {
                     result = new VolumeBackupMetadataMaker().syncVolumeBackupsMetaDataFromImageStore(msg.getImageStoreUuid());
                } catch (IOException e) {
                    event.setError(operr("sync volume backup metadata file in image store[uuid:%s] meet I/O error: %s",
                            msg.getImageStoreUuid(), e.getMessage()));
                    logger.error("sync volume backup failure", e);
                }

                event.setResult(result);
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APISyncVmBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "sync-backup";
            }

            @Override
            public void run(SyncTaskChain chain) {
                APISyncVmBackupEvent event = new APISyncVmBackupEvent(msg.getId());
                SyncBackupResult result = null;
                try {
                    result = new VolumeBackupMetadataMaker().syncVmBackupsMetaDataFromImageStore(msg.getImageStoreUuid());
                } catch (IOException e) {
                    event.setError(operr("sync volume backup metadata file in image store[uuid:%s] meet I/O error: %s",
                            msg.getImageStoreUuid(), e.getMessage()));
                    logger.error("sync volume backup failure", e);
                }

                event.setResult(result);
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private String getVolumeHypervisor(String volumeUuid) {
        return SQL.New("select distinct(c.hypervisorType) from VolumeVO vol, VmInstanceVO vm, ClusterVO c "
                + "where vol.uuid = :voluuid "
                + "and vm.uuid = vol.vmInstanceUuid "
                + "and c.uuid = vm.clusterUuid", String.class)
                .param("voluuid", volumeUuid)
                .find();
    }

    private String getVmInstanceUuid(String backupUuid) {
        List<Tuple> vmUuidTuples = SQL.New("select vol.vmInstanceUuid, bak.vmInstanceUuid from VolumeVO vol, VolumeBackupVO bak "
                        + "where bak.uuid = :backupUuid "
                        + "and vol.uuid = bak.volumeUuid ", Tuple.class)
                .param("backupUuid", backupUuid)
                .list();

        String volumeVmUuid = vmUuidTuples.get(0).get(0, String.class);
        String backupVmUuid = vmUuidTuples.get(0).get(1, String.class);

        if (volumeVmUuid != null && backupVmUuid != null && !backupVmUuid.equals(volumeVmUuid)) {
            throw new ApiMessageInterceptionException(operr("Current vm[uuid: %s] of the backup volume is no longer the vm[uuid: %s] that was used for backup",
                    volumeVmUuid, backupUuid, backupVmUuid));
        }
        return volumeVmUuid == null ? backupVmUuid : volumeVmUuid;
    }

    private VolumeBackupFactory getHypervisorBackupFactory(String hypervisorType) {
        VolumeBackupFactory factory = bkFactories.get(hypervisorType);
        if (factory == null) {
            throw new OperationFailureException(Platform.operr("No VolumeBackupFactory of type[%s] found", hypervisorType));
        }
        return factory;
    }

    private void passThrough(VolumeBackupMessage msg) {
        String hypervisor = getVolumeHypervisor(msg.getVolumeUuid());
        if (StringUtils.isNullOrEmpty(hypervisor)) {
            String err = String.format("hypervisor not found for volume: %s", msg.getVolumeUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        if (Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.state, BackupStorageState.Disabled)
                .eq(BackupStorageVO_.uuid, msg.getBackupStorageUuid())
                .isExists()) {
            bus.replyErrorByMessageType((Message) msg, operr("One of the backup storage[uuid: %s] is in the state of %s, can not do sync operation",
                    msg.getBackupStorageUuid(), BackupStorageState.Disabled.toString()));
            return;
        }

        VolumeBackupFactory factory = this.getHypervisorBackupFactory(hypervisor);
        VolumeBackupHypervisorBackend bkd = factory.getHypervisorBackend();
        bkd.handleMessage(msg);
    }

    private List<DeleteBitsOnBackupStorageMsg> buildMsgs(String backupUuid, List<String> backupStorageUuids) {
        Q q = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                .select(VolumeBackupStorageRefVO_.installPath)
                .select(VolumeBackupStorageRefVO_.backupStorageUuid);

        List<Tuple> ts;

        if (backupStorageUuids == null || backupStorageUuids.isEmpty()) {
            ts = q.listTuple();
        } else {
            ts = q.in(VolumeBackupStorageRefVO_.backupStorageUuid, backupStorageUuids).listTuple();
        }

        return ts.stream().map(t -> {
            DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
            dmsg.setInstallPath(t.get(0, String.class));
            dmsg.setBackupStorageUuid(t.get(1, String.class));
            bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
            return dmsg;
        }).collect(Collectors.toList());
    }

    private void doDeleteVolumeBackup(VolumeBackupDeleteMessage msg,
                                      List<String> backupStorageUuids,
                                      Completion completion) {

        VolumeBackupVO backup = dbf.findByUuid(msg.getVolumeBackupUuid(), VolumeBackupVO.class);
        if (backup == null) {
            completion.success();
            return;
        }

        if (CollectionUtils.isEmpty(backup.getBackupStorageRefs())) {
            dbf.remove(backup);
            completion.success();
            return;
        }

        String backupUuid = backup.getUuid();
        While.Do<DeleteBitsOnBackupStorageMsg> consumer, msgSender;
        BiFunction<String, String, Boolean> otherDelMsgBuilder;
        Map<String, VolumeBackupDeletionMsg> otherMsgs = new HashMap<>();
        Map<String, List<String>> backupChains = groupBackupByChain(backup.getVolumeUuid());
        List<String> deletedBsUuids = Collections.synchronizedList(new ArrayList<>());
        msgSender = (dmsg, compl) -> bus.send(dmsg, new CloudBusCallBack(compl) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(reply.getError().getReadableDetails());
                }

                SQL.New(VolumeBackupStorageRefVO.class)
                        .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                        .eq(VolumeBackupStorageRefVO_.backupStorageUuid, dmsg.getBackupStorageUuid())
                        .eq(VolumeBackupStorageRefVO_.installPath, dmsg.getInstallPath())
                        .delete();
                deletedBsUuids.add(dmsg.getBackupStorageUuid());
                compl.done();
            }
        });

        otherDelMsgBuilder = (bsUuid, chainName) -> {
            List<String> chain = backupChains.get(chainName);
            Map<String, VolumeBackupStatus> otherBackupStatus = Q.New(VolumeBackupStorageRefVO.class)
                    .select(VolumeBackupStorageRefVO_.volumeBackupUuid, VolumeBackupStorageRefVO_.status)
                    .in(VolumeBackupStorageRefVO_.volumeBackupUuid, chain)
                    .notEq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                    .eq(VolumeBackupStorageRefVO_.backupStorageUuid, bsUuid)
                    .listTuple().stream().collect(Collectors.toMap(
                            t -> t.get(0, String.class),
                            t -> t.get(1, VolumeBackupStatus.class)
                    ));

            if (otherBackupStatus.values().stream().anyMatch(s -> s != VolumeBackupStatus.Deleted)) {
                return false;
            }

            // delete chain
            for (String toDeleteBackupUuid : otherBackupStatus.keySet()) {
                if (otherMsgs.containsKey(toDeleteBackupUuid)) {
                    otherMsgs.get(toDeleteBackupUuid).getBackupStorageUuids().add(bsUuid);
                } else {
                    VolumeBackupDeletionMsg delMsg = new VolumeBackupDeletionMsg();
                    delMsg.setBackupStorageUuids(new ArrayList<>(Collections.singletonList(bsUuid)));
                    delMsg.setUuid(toDeleteBackupUuid);
                    bus.makeTargetServiceIdByResourceUuid(delMsg, VolumeBackupConstant.SERVICE_ID, backup.getVolumeUuid());
                    otherMsgs.put(toDeleteBackupUuid, delMsg);
                }
            }

            logger.debug(String.format("delete whole backup chain[name:%s, backupUuid:%s bsUuid:%s]", chainName, chain, bsUuid));
            return true;
        };

        if (msg.isHandleDependency()) {
            consumer = (dmsg, compl) -> {
                String chainName = getBackupChainName(dmsg.getInstallPath());
                List<String> chain = backupChains.get(chainName);
                boolean deleteWholeChain = otherDelMsgBuilder.apply(dmsg.getBackupStorageUuid(), chainName);
                if (CollectionUtils.isEmpty(chain)
                        || chain.get(0).equals(backupUuid) // backup is top of chain
                        || deleteWholeChain) {
                    msgSender.accept(dmsg, compl);
                    return;
                }

                compl.done();
            };
        } else {
            consumer = (dmsg, compl) -> {
                String chainName = getBackupChainName(dmsg.getInstallPath());
                otherDelMsgBuilder.apply(dmsg.getBackupStorageUuid(), chainName);
                msgSender.accept(dmsg, compl);
            };
        }

        List<DeleteBitsOnBackupStorageMsg> dmsgs = buildMsgs(backupUuid, backupStorageUuids);
        updateBackupStatus(Collections.singletonList(backupUuid),
                dmsgs.stream().map(DeleteBitsOnBackupStorageMsg::getBackupStorageUuid).collect(Collectors.toList()),
                VolumeBackupStatus.Deleted);
        new While<>(dmsgs).step(consumer, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        List<VolumeBackupStorageRefVO> refs = q(VolumeBackupStorageRefVO.class)
                                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                                .list();

                        if (refs.isEmpty()) {
                            sql(VolumeBackupVO.class).eq(VolumeBackupVO_.uuid, backupUuid).delete();
                        } else if (refs.stream().allMatch(it -> it.getStatus() == VolumeBackupStatus.Deleted)
                                && backup.getStatus() != VolumeBackupStatus.Deleted) {
                            sql(VolumeBackupVO.class)
                                    .eq(VolumeBackupVO_.uuid, backupUuid)
                                    .set(VolumeBackupVO_.status, VolumeBackupStatus.Deleted)
                                    .update();
                            logger.debug(String.format("update backup[uuid: %s] status to Deleted", backupUuid));
                        }
                    }
                }.execute();
                afterDeleteVolumeBackup();
                if (!otherMsgs.isEmpty()) {
                    bus.send(new ArrayList<>(otherMsgs.values()));
                }
                completion.success();
            }

            private void afterDeleteVolumeBackup() {
                if (deletedBsUuids.isEmpty()) {
                    return;
                }

                pluginRgty.getExtensionList(DeleteVolumeBackupExtensionPoint.class).forEach(new Consumer<DeleteVolumeBackupExtensionPoint>() {
                    @Override
                    @ExceptionSafe
                    public void accept(DeleteVolumeBackupExtensionPoint ext) {
                        ext.afterDeleteVolumeBackup(backupUuid, deletedBsUuids);
                    }
                });
            }
        });
    }

    static private String buildImageUrl(String installPath, String bsUuid) {
        final String hostname = Q.New(ImageStoreBackupStorageVO.class)
                .select(ImageStoreBackupStorageVO_.hostname)
                .eq(ImageStoreBackupStorageVO_.uuid, bsUuid)
                .findValue();
        final String p = ImageStoreBackupStorage.zstoreProto;
        return p + hostname + installPath.substring(p.length()-1);
    }

    static private String getVolumePlatform(String volumeUuid) {
        String platform = SQL.New("select vm.platform from VmInstanceVO vm, VolumeVO vol "
                + "where vol.uuid = :volumeUuid "
                + "and vol.vmInstanceUuid = vm.uuid", String.class)
                .param("volumeUuid", volumeUuid)
                .find();
        return platform == null ? ImagePlatform.Other.toString() : platform;
    }

    static private String getClusterArchOrElseDefault(String clusterUuid) {
        String clusterArch = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, clusterUuid)
                .select(ClusterVO_.architecture)
                .findValue();
        return clusterArch != null ? clusterArch : ImageArchitecture.defaultArch();
    }

    private List<String> buildSystemTags() {
        final String ca = new JsonLabel().get("imageStoreCA", String.class);
        final String tag = String.format("image::cert::%s", ca);
        return Collections.singletonList(tag);
    }

    private TemplateCreateStruct makeCreateTemplateStruct(String backupUuid, String bsUuid, SessionInventory session) {
        return makeCreateTemplateStruct(dbf.findByUuid(backupUuid, VolumeBackupVO.class), bsUuid, session);
    }

    private TemplateCreateStruct makeCreateTemplateStruct(VolumeBackupVO vo, String bsUuid, SessionInventory session) {
        VolumeMetadata metadata = VolumeMetaDataMaker.parseVolumeMetadata(vo.getMetadata());
        TemplateCreateStruct struct = new TemplateCreateStruct();

        struct.setName("create-from-backup-" + vo.getUuid());
        struct.setDescription("Created from backup: " + vo.getUuid());
        struct.setSession(session);
        struct.setBackupUuid(vo.getUuid());
        struct.setBackupStorageUuid(bsUuid);
        struct.setPlatform(metadata.getPlatformOrElse(() -> getVolumePlatform(vo.getVolumeUuid())));
        struct.setVmSystemTags(metadata.getVmSystemTags());
        struct.setArchitecture(metadata.getArchitectureOrElse(() -> getClusterArchOrElseDefault(metadata.getClusterUuid())));
        struct.setGuestOsType(metadata.getGuestOsType());
        struct.setVirtio(metadata.isVirtio());

        return struct;
    }

    private TemplateCreateStruct makeCreateTemplateStruct(CreateVolumeTemplateFromBackupMessage message) {
        TemplateCreateStruct struct = new TemplateCreateStruct();
        Tuple t = Q.New(VolumeBackupVO.class).select(VolumeBackupVO_.metadata, VolumeBackupVO_.volumeUuid)
                .eq(VolumeBackupVO_.uuid, message.getBackupUuid())
                .findTuple();
        VolumeMetadata metadata = VolumeMetaDataMaker.parseVolumeMetadata(t.get(0, String.class));
        struct.setVmSystemTags(metadata.getVmSystemTags());
        struct.setArchitecture(message.getArchitecture() != null ? message.getArchitecture():
                metadata.getArchitectureOrElse(() -> getClusterArchOrElseDefault(metadata.getClusterUuid())
                ));
        struct.setPlatform(message.getPlatform() != null ? message.getPlatform() :
                metadata.getPlatformOrElse(() -> getVolumePlatform(t.get(1, String.class))
                ));

        struct.setName(message.getName());
        struct.setBackupStorageUuid(message.getBackupStorageUuid());
        struct.setBackupUuid(message.getBackupUuid());
        struct.setDescription(message.getDescription());
        struct.setVirtio(message.getVirtio() != null ? message.getVirtio() : metadata.isVirtio());
        struct.setGuestOsType(message.getGuestOsType() != null ? message.getGuestOsType() : metadata.getGuestOsType());
        struct.setSession(message.getSession());
        struct.setSystem(message.isSystem());
        struct.setResourceUuid(message.getResourceUuid());

        return struct;
    }

    private void doCreateTemplateFromBackup(TemplateCreateStruct struct,
                                            ImageConstant.ImageMediaType type,
                                            ReturnValueCompletion<ImageInventory> completion) {
        String installPath = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, struct.getBackupUuid())
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, struct.getBackupStorageUuid())
                .select(VolumeBackupStorageRefVO_.installPath)
                .findValue();
        if (installPath == null) {
            completion.fail(Platform.operr("Volume backup[uuid:%s] not found on backup storage[uuid:%s]",
                    struct.getBackupUuid(), struct.getBackupStorageUuid()));
            return;
        }

        AddImageMsg amsg = new AddImageMsg();
        amsg.setSession(struct.getSession());
        amsg.setResourceUuid(struct.getResourceUuid());
        amsg.setName(struct.getName());
        amsg.setDescription(struct.getDescription());
        amsg.setBackupStorageUuids(Collections.singletonList(struct.getBackupStorageUuid()));
        amsg.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        amsg.setMediaType(type.toString());
        amsg.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        amsg.setSystem(struct.isSystem());
        amsg.setGuestOsType(struct.getGuestOsType());
        amsg.setVirtio(struct.isVirtio());
        amsg.setSystemTags(buildSystemTags());
        amsg.setPlatform(struct.getPlatform());
        amsg.setArchitecture(struct.getArchitecture());
        amsg.setUrl(buildImageUrl(installPath, struct.getBackupStorageUuid()));

        bus.makeLocalServiceId(amsg, ImageConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                AddImageReply r = reply.castReply();
                syncSystemTag(r.getInventory().getUuid(), new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        completion.success(r.getInventory());
                    }
                });
            }

            private void syncSystemTag(String imageUuid, NoErrorCompletion compl) {
                if (type != ImageConstant.ImageMediaType.RootVolumeTemplate ||
                        struct.getVmSystemTags() == null || struct.getVmSystemTags().isEmpty()) {
                    compl.done();
                    return;
                }

                SyncSystemTagFromTagMsg smsg = new SyncSystemTagFromTagMsg();
                smsg.setImageUuid(imageUuid);
                smsg.setVmSystemTags(struct.getVmSystemTags());
                bus.makeLocalServiceId(smsg, ImageConstant.SERVICE_ID);
                bus.send(smsg, new CloudBusCallBack(compl) {
                    @Override
                    public void run(MessageReply reply) {
                        compl.done();
                    }
                });
            }
        });
    }

    private void handle(final APICreateRootVolumeTemplateFromVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("volume-backup-%s", msg.getBackupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateRootVolumeTemplateFromVolumeBackupEvent evt = new APICreateRootVolumeTemplateFromVolumeBackupEvent(msg.getId());

                doCreateTemplateFromBackup(makeCreateTemplateStruct(msg),
                        ImageConstant.ImageMediaType.RootVolumeTemplate,
                        new ReturnValueCompletion<ImageInventory>(msg, chain) {
                            @Override
                            public void success(ImageInventory inventory) {
                                evt.setInventory(inventory);
                                bus.publish(evt);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                evt.setError(errorCode);
                                bus.publish(evt);
                                chain.next();
                            }
                        }
                );
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final APICreateDataVolumeTemplateFromVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("volume-backup-%s", msg.getBackupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateDataVolumeTemplateFromVolumeBackupEvent evt = new APICreateDataVolumeTemplateFromVolumeBackupEvent(msg.getId());

                doCreateTemplateFromBackup(makeCreateTemplateStruct(msg),
                        ImageConstant.ImageMediaType.DataVolumeTemplate,
                        new ReturnValueCompletion<ImageInventory>(msg, chain) {
                            @Override
                            public void success(ImageInventory inventory) {
                                evt.setInventory(inventory);
                                bus.publish(evt);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                evt.setError(errorCode);
                                bus.publish(evt);
                                chain.next();
                            }
                        }
                );
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final APIDeleteVolumeBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("volume-backup-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteVolumeBackupEvent evt = new APIDeleteVolumeBackupEvent(msg.getId());
                doDeleteVolumeBackup(msg,  msg.getBackupStorageUuids(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final APIDeleteVmBackupMsg msg) {
        APIDeleteVmBackupEvent evt = new APIDeleteVmBackupEvent(msg.getId());
        List<VolumeBackupVO> vos = verifyGroupUuidForDelete(msg.getGroupUuid(), msg.getSession());
        if (CollectionUtils.isEmpty(vos)) {
            bus.publish(evt);
            return;
        }

        new While<>(vos).each((vo, whileCompletion) -> {
            VolumeBackupDeletionMsg dmsg = new VolumeBackupDeletionMsg();
            dmsg.setBackupStorageUuids(msg.getBackupStorageUuids());
            dmsg.setUuid(vo.getUuid());
            dmsg.setHandleDependency(msg.isHandleDependency());
            bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeBackupConstant.SERVICE_ID, vo.getVolumeUuid());
            bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("delete volume backup failed: %s", reply.getError().getDetails()));
                    }

                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                for (CleanUpVmBackupExtensionPoint ext : pluginRgty.getExtensionList(CleanUpVmBackupExtensionPoint.class)) {
                    ext.afterCleanUpVmBackup(msg.getGroupUuid());
                }
                bus.publish(evt);
            }
        });
    }

    private void deleteImageAsync(String imageUuid, String bsUuid) {
        ImageDeletionMsg dmsg = new ImageDeletionMsg();
        dmsg.setImageUuid(imageUuid);
        dmsg.setBackupStorageUuids(bsUuid != null ? Collections.singletonList(bsUuid) : null);
        dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, imageUuid);
        bus.send(dmsg);
    }

    private void waitUntilReady(final String bsUuid, final String taskId, FlowTrigger trigger) {
        final long interval = 3;

        if (taskId.equals("existed")) {
            trigger.next();
            return;
        }

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask(trigger) {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return String.format("wait-until-task-ready-for-taskid-%s", taskId);
            }

            @Override
            public boolean run() {
                GetSyncTaskStatusMsg gmsg = new GetSyncTaskStatusMsg();
                gmsg.setBsUuid(bsUuid);
                gmsg.setTaskId(taskId);
                bus.makeLocalServiceId(gmsg, ImageStoreBackupStorageConstant.SERVICE_ID);

                GetSyncTaskStatusReply reply = bus.call(gmsg).castReply();
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return true;
                }

                switch (reply.getStatus()) {
                    case TsRunning:
                    case TsWaiting:
                        // continue waiting
                        logger.debug(String.format("sync task[%s] status: %s", taskId, reply.getStatus()));
                        return false;
                    case TsFailed:
                        trigger.fail(Platform.operr("sync task failed."));
                        return true;
                    case TsSuccess:
                        trigger.next();
                        return true;
                }

                trigger.fail(Platform.operr("unexpected task status: %s", reply.getStatus()));
                return true;
            }
        });
    }

    private void handle(APIRecoverBackupFromImageStoreBackupStorageMsg msg) {
        APIRecoverBackupFromImageStoreBackupStorageEvent evt =
                new APIRecoverBackupFromImageStoreBackupStorageEvent(msg.getId());
        recoverBackupInQueue(msg.getUuid(),
                msg.getSrcBackupStorageUuid(),
                msg.getDstBackupStorageUuid(),
                new ReturnValueCompletion<VolumeBackupInventory>(msg) {
                    @Override
                    public void success(VolumeBackupInventory inv) {
                        evt.setInventory(inv);
                        bus.publish(evt);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                    }
                });
    }

    private void recoverBackupInQueue(String backupUuid,
                                      String srcBackupStorageUuid,
                                      String dstBackupStorageUuid,
                                      ReturnValueCompletion<VolumeBackupInventory> completion) {

        String volumeUuid = Q.New(VolumeBackupVO.class)
                .select(VolumeBackupVO_.volumeUuid)
                .eq(VolumeBackupVO_.uuid, backupUuid)
                .findValue();
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-volume-%s-backup", volumeUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doRecoverBackup(backupUuid, srcBackupStorageUuid, dstBackupStorageUuid,
                        new ReturnValueCompletion<VolumeBackupInventory>(completion) {
                            @Override
                            public void success(VolumeBackupInventory inv) {
                                completion.success(inv);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                                chain.next();
                            }
                        });

            }

            @Override
            public String getName() {
                return String.format("recover-backup-%s-from-%s-to-%s", volumeUuid, dstBackupStorageUuid, srcBackupStorageUuid);
            }
        });
    }


    private void doRecoverBackup(String backupUuid,
                                 String srcBackupStorageUuid,
                                 String dstBackupStorageUuid,
                                 ReturnValueCompletion<VolumeBackupInventory> completion) {
        final String installPath = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, dstBackupStorageUuid)
                .select(VolumeBackupStorageRefVO_.installPath)
                .findValue();
        if (installPath == null) {
            completion.fail(Platform.operr("volume backup[uuid:%s] not found in backup storage[uuid:%s]",
                    backupUuid, dstBackupStorageUuid));
            return;
        }

        VolumeBackupStorageRefVO refVO = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, srcBackupStorageUuid).find();
        if (refVO != null) {
            if (refVO.getStatus() == VolumeBackupStatus.Deleted) {
                refVO.setStatus(VolumeBackupStatus.Ready);
                dbf.update(refVO);
            }

            VolumeBackupVO vbvo = dbf.findByUuid(backupUuid, VolumeBackupVO.class);
            if (vbvo.getStatus() == VolumeBackupStatus.Deleted) {
                vbvo.setStatus(VolumeBackupStatus.Ready);
                dbf.update(vbvo);
            }

            completion.success(VolumeBackupInventory.valueOf(vbvo));
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("pull-backup-%s-from-dst-to-src-imagestore", backupUuid));
        chain.then(new ShareFlow() {
            VolumeBackupStorageRefVO refVO;
            String taskId;
            List<String> chainInBs;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "before-create-volume-backup-extension";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeBackupVO vo = dbf.findByUuid(backupUuid, VolumeBackupVO.class);
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(it ->
                                it.beforeCreateVolumeBackup(vo, srcBackupStorageUuid)
                        );
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow(){
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refVO = new VolumeBackupStorageRefVO();
                        refVO.setVolumeBackupUuid(backupUuid);
                        refVO.setBackupStorageUuid(srcBackupStorageUuid);
                        refVO.setInstallPath(installPath);
                        refVO.setStatus(VolumeBackupStatus.Creating);
                        refVO = dbf.persist(refVO);
                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "pull-backup-" + backupUuid;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PullBitsBetweenImageStoreMsg pmsg = new PullBitsBetweenImageStoreMsg();
                        pmsg.setDstImageStorageUuid(dstBackupStorageUuid);
                        pmsg.setSrcImageStorageUuid(srcBackupStorageUuid);
                        pmsg.setInstallPath(installPath);

                        bus.makeTargetServiceIdByResourceUuid(pmsg, ImageStoreBackupStorageConstant.SERVICE_ID, srcBackupStorageUuid);
                        bus.send(pmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    PullBitsBetweenImageStoreReply reply1 = reply.castReply();
                                    taskId = reply1.getTaskId();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (refVO != null) {
                            dbf.remove(refVO);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "wait-pull-until-success";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        waitUntilReady(srcBackupStorageUuid, taskId, trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-backing-chain";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetImageChainInfoMsg gmsg = new GetImageChainInfoMsg();
                        gmsg.setBackupStorageUuid(srcBackupStorageUuid);
                        gmsg.setInstallPath(installPath);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, srcBackupStorageUuid);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                GetImageChainInfoReply r = reply.castReply();
                                if (r.getChain() != null) {
                                    chainInBs = r.getChain().stream().map(ImageStoreImageResponse::getInstallPath).collect(Collectors.toList());
                                }

                                trigger.next();
                            }
                        });
                    }
                });


                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        refVO.setStatus(VolumeBackupStatus.Ready);
                        dbf.updateAndRefresh(refVO);

                        VolumeBackupVO vbvo = dbf.findByUuid(backupUuid, VolumeBackupVO.class);
                        recoverBackingChainRef();
                        afterCreateBackup(vbvo);
                        completion.success(VolumeBackupInventory.valueOf(vbvo));
                    }

                    private void recoverBackingChainRef() {
                        if (CollectionUtils.isEmpty(chainInBs)) {
                            return;
                        }

                        List<Tuple> ts = Q.New(VolumeBackupStorageRefVO.class)
                                .select(VolumeBackupStorageRefVO_.installPath, VolumeBackupStorageRefVO_.backupStorageUuid, VolumeBackupStorageRefVO_.volumeBackupUuid)
                                .in(VolumeBackupStorageRefVO_.backupStorageUuid, Arrays.asList(dstBackupStorageUuid, srcBackupStorageUuid))
                                .in(VolumeBackupStorageRefVO_.installPath, chainInBs)
                                .listTuple();

                        Map<String, Set<String>> backupLocationsInDb = ts.stream().collect(Collectors.
                                groupingBy(t -> t.get(0, String.class), Collectors.mapping(it -> it.get(1, String.class), Collectors.toSet()))
                        );

                        Map<String, String> backupUuids = ts.stream().collect(Collectors.toMap(
                                t -> t.get(0, String.class),
                                t -> t.get(2, String.class), (o, n) -> o
                        ));

                        // create new ref for in src chain but not in dst chain
                        List<VolumeBackupStorageRefVO> toPersists = new ArrayList<>();
                        chainInBs.retainAll(backupLocationsInDb.keySet());
                        chainInBs.forEach(imagePath -> {
                            if (!backupLocationsInDb.get(imagePath).contains(srcBackupStorageUuid) &&
                                    backupLocationsInDb.get(imagePath).contains(dstBackupStorageUuid)) {
                                VolumeBackupStorageRefVO ref = new VolumeBackupStorageRefVO();
                                ref.setBackupStorageUuid(dstBackupStorageUuid);
                                ref.setInstallPath(imagePath);
                                ref.setStatus(VolumeBackupStatus.Deleted);
                                ref.setVolumeBackupUuid(backupUuids.get(imagePath));
                                toPersists.add(ref);
                                logger.debug(String.format("create new ref[bsUuid:%s, volumeBackupUuid:%s] for recover chain",
                                        dstBackupStorageUuid, backupUuids.get(imagePath)));
                            }
                        });
                        if (!toPersists.isEmpty()) {
                            dbf.persistCollection(toPersists);
                        }
                    }

                    private void afterCreateBackup(VolumeBackupVO vo) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(new Consumer<CreateVolumeBackupExtensionPoint>() {
                            @Override
                            @ExceptionSafe
                            public void accept(CreateVolumeBackupExtensionPoint ext) {
                                ext.afterCreateVolumeBackup(vo, srcBackupStorageUuid);
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void syncBackupInQueue(String backupUuid,
                                   String srcBackupStorageUuid,
                                   String dstBackupStorageUuid,
                                   ReturnValueCompletion<VolumeBackupInventory> completion) {

        String volumeUuid = Q.New(VolumeBackupVO.class)
                .select(VolumeBackupVO_.volumeUuid)
                .eq(VolumeBackupVO_.uuid, backupUuid)
                .findValue();
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-volume-%s-backup", volumeUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSync(backupUuid, srcBackupStorageUuid, dstBackupStorageUuid,
                        new ReturnValueCompletion<VolumeBackupInventory>(completion) {
                            @Override
                            public void success(VolumeBackupInventory inv) {
                                completion.success(inv);
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                                chain.next();
                            }
                        });
            }

            @Override
            public String getName() {
                return String.format("sync-backup-%s-from-%s-to-%s", volumeUuid, srcBackupStorageUuid, dstBackupStorageUuid);
            }
        });
    }

    private void doSync(String backupUuid,
                        String srcBackupStorageUuid,
                        String dstBackupStorageUuid,
                        ReturnValueCompletion<VolumeBackupInventory> completion) {
        final String installPath = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, srcBackupStorageUuid)
                .select(VolumeBackupStorageRefVO_.installPath)
                .findValue();
        if (installPath == null) {
            completion.fail(Platform.operr("volume backup[uuid:%s] not found in backup storage[uuid:%s]",
                    backupUuid, srcBackupStorageUuid));
            return;
        }

        VolumeBackupVO vbvo = dbf.findByUuid(backupUuid, VolumeBackupVO.class);
        VolumeBackupStorageRefVO refVO = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuid)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, dstBackupStorageUuid)
                .find();
        if (refVO != null) {
            if (refVO.getStatus() == VolumeBackupStatus.Deleted) {
                refVO.setStatus(VolumeBackupStatus.Ready);
                dbf.update(refVO);
            }

            if (vbvo.getStatus() == VolumeBackupStatus.Deleted) {
                vbvo.setStatus(VolumeBackupStatus.Ready);
                dbf.update(vbvo);
            }

            completion.success(VolumeBackupInventory.valueOf(vbvo));
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("push-backup-%s-from-src-to-dst-imagestore", backupUuid));
        chain.then(new ShareFlow() {
            VolumeBackupStorageRefVO refVO;
            String taskId;
            List<String> chainInBs;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "before-create-volume-backup-extension";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(it ->
                                it.beforeCreateVolumeBackup(vbvo, dstBackupStorageUuid)
                        );
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "persist-db";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refVO = new VolumeBackupStorageRefVO();
                        refVO.setVolumeBackupUuid(backupUuid);
                        refVO.setBackupStorageUuid(dstBackupStorageUuid);
                        refVO.setStatus(VolumeBackupStatus.Creating);
                        refVO.setInstallPath(installPath);
                        refVO = dbf.persist(refVO);
                        trigger.next();
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "get-backing-chain";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetImageChainInfoMsg gmsg = new GetImageChainInfoMsg();
                        gmsg.setBackupStorageUuid(srcBackupStorageUuid);
                        gmsg.setInstallPath(installPath);
                        bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, srcBackupStorageUuid);
                        bus.send(gmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                GetImageChainInfoReply r = reply.castReply();
                                if (r.getChain() != null) {
                                    chainInBs = r.getChain().stream().map(ImageStoreImageResponse::getInstallPath).collect(Collectors.toList());
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "push-backup-" + backupUuid;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PushBitsBetweenImageStoreMsg pmsg = new PushBitsBetweenImageStoreMsg();
                        pmsg.setDstImageStorageUuid(dstBackupStorageUuid);
                        pmsg.setSrcImageStorageUuid(srcBackupStorageUuid);
                        pmsg.setInstallPath(installPath);

                        bus.makeTargetServiceIdByResourceUuid(pmsg, ImageStoreBackupStorageConstant.SERVICE_ID, srcBackupStorageUuid);
                        bus.send(pmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    PushBitsBetweenImageStoreReply reply1 = reply.castReply();
                                    taskId = reply1.getTaskId();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (refVO != null) {
                            dbf.remove(refVO);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "wait-push-until-success";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        waitUntilReady(srcBackupStorageUuid, taskId, trigger);
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        refVO.setStatus(VolumeBackupStatus.Ready);
                        dbf.updateAndRefresh(refVO);

                        VolumeBackupVO vbvo = dbf.findByUuid(backupUuid, VolumeBackupVO.class);
                        if (vbvo.getStatus() != VolumeBackupStatus.Ready) {
                            vbvo.setStatus(VolumeBackupStatus.Ready);
                            dbf.update(vbvo);
                        }

                        syncBackingChainRef();
                        afterCreateBackup(vbvo);
                        completion.success(VolumeBackupInventory.valueOf(vbvo));
                    }

                    private void syncBackingChainRef() {
                        if (CollectionUtils.isEmpty(chainInBs)) {
                            return;
                        }

                        List<Tuple> ts = Q.New(VolumeBackupStorageRefVO.class)
                                .select(VolumeBackupStorageRefVO_.installPath, VolumeBackupStorageRefVO_.backupStorageUuid, VolumeBackupStorageRefVO_.volumeBackupUuid)
                                .in(VolumeBackupStorageRefVO_.backupStorageUuid, Arrays.asList(dstBackupStorageUuid, srcBackupStorageUuid))
                                .in(VolumeBackupStorageRefVO_.installPath, chainInBs)
                                .listTuple();

                        Map<String, Set<String>> backupLocationsInDb = ts.stream().collect(Collectors.
                                groupingBy(t -> t.get(0, String.class), Collectors.mapping(it -> it.get(1, String.class), Collectors.toSet()))
                        );

                        Map<String, String> backupUuids = ts.stream().collect(Collectors.toMap(
                                t -> t.get(0, String.class),
                                t -> t.get(2, String.class), (o, n) -> o
                        ));

                        // create new ref for in src chain but not in dst chain
                        List<VolumeBackupStorageRefVO> toPersists = new ArrayList<>();
                        chainInBs.retainAll(backupLocationsInDb.keySet());
                        chainInBs.forEach(imagePath -> {
                            if (backupLocationsInDb.get(imagePath).contains(srcBackupStorageUuid) &&
                                    !backupLocationsInDb.get(imagePath).contains(dstBackupStorageUuid)) {
                                VolumeBackupStorageRefVO ref = new VolumeBackupStorageRefVO();
                                ref.setBackupStorageUuid(dstBackupStorageUuid);
                                ref.setInstallPath(imagePath);
                                ref.setStatus(VolumeBackupStatus.Deleted);
                                ref.setVolumeBackupUuid(backupUuids.get(imagePath));
                                toPersists.add(ref);
                                logger.debug(String.format("create new ref[bsUuid:%s, volumeBackupUuid:%s] for sync chain",
                                        dstBackupStorageUuid, backupUuids.get(imagePath)));
                            }
                        });
                        if (!toPersists.isEmpty()) {
                            dbf.persistCollection(toPersists);
                        }
                    }

                    private void afterCreateBackup(VolumeBackupVO vo) {
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(new Consumer<CreateVolumeBackupExtensionPoint>() {
                            @Override
                            @ExceptionSafe
                            public void accept(CreateVolumeBackupExtensionPoint ext) {
                                ext.afterCreateVolumeBackup(vo, dstBackupStorageUuid);
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        VolumeBackupVO vo = dbf.findByUuid(backupUuid, VolumeBackupVO.class);
                        pluginRgty.getExtensionList(CreateVolumeBackupExtensionPoint.class).forEach(it ->
                                it.failedToCreateVolumeBackup(vo, dstBackupStorageUuid)
                        );
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(SyncBackupFromImageStoreBackupStorageMsg msg) {
        SyncBackupFromImageStoreBackupStorageReply reply = new SyncBackupFromImageStoreBackupStorageReply();

        if (Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.state, BackupStorageState.Disabled)
                .in(BackupStorageVO_.uuid, Arrays.asList(msg.getDstBackupStorageUuid(), msg.getSrcBackupStorageUuid()))
                .isExists()) {
            reply.setError(operr("One of the backup storage[uuids: %s, %s] is in the state of %s, can not do sync operation",
                    msg.getDstBackupStorageUuid(), msg.getSrcBackupStorageUuid(), BackupStorageState.Disabled.toString()));
            bus.reply(msg, reply);
            return;
        }

        syncBackupInQueue(msg.getUuid(),
                msg.getSrcBackupStorageUuid(),
                msg.getDstBackupStorageUuid(),
                new ReturnValueCompletion<VolumeBackupInventory>(msg) {
                    @Override
                    public void success(VolumeBackupInventory inv) {
                        reply.setInventory(inv);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void handle(APISyncBackupFromImageStoreBackupStorageMsg msg) {
        APISyncBackupFromImageStoreBackupStorageEvent evt = new APISyncBackupFromImageStoreBackupStorageEvent(msg.getId());
        syncBackupInQueue(msg.getUuid(),
                msg.getSrcBackupStorageUuid(),
                msg.getDstBackupStorageUuid(),
                new ReturnValueCompletion<VolumeBackupInventory>(msg) {
                    @Override
                    public void success(VolumeBackupInventory inv) {
                        evt.setInventory(inv);
                        bus.publish(evt);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                    }
                });
    }

    private void handle(APIRevertVolumeFromVolumeBackupMsg msg) {
        VolumeType volType = Q.New(VolumeBackupVO.class)
                .eq(VolumeBackupVO_.uuid, msg.getUuid())
                .select(VolumeBackupVO_.type)
                .findValue();

        Completion comp = new Completion(msg) {
            final APIRevertVolumeFromVolumeBackupEvent evt = new APIRevertVolumeFromVolumeBackupEvent(msg.getId());

            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        };

        if (volType == VolumeType.Root) {
            doRevertRootVolume(msg.getUuid(), msg.getBackupStrogeUuid(), msg.getSession(), comp);
        } else {
            doRevertDataVolume(msg.getUuid(), msg.getBackupStrogeUuid(), msg.getSession(), comp);
        }
    }

    private List<VolumeBackupVO> verifyGroupUuidForDelete(final String groupUuid, SessionInventory session) {
        List<VolumeBackupVO> vos = Q.New(VolumeBackupVO.class)
                .eq(VolumeBackupVO_.groupUuid, groupUuid)
                .list();
        if (vos.isEmpty()) {
            return Collections.emptyList();
        }

        return verifyGroup(vos, session);
    }

    private List<VolumeBackupVO> verifyGroupUuid(final String groupUuid, SessionInventory session) {
        List<VolumeBackupVO> vos = Q.New(VolumeBackupVO.class)
                .eq(VolumeBackupVO_.groupUuid, groupUuid)
                .list();
        if (vos.isEmpty()) {
            throw new OperationFailureException(
                    err(SysErrors.RESOURCE_NOT_FOUND, "No volume backups found with group uuid: %s", groupUuid)
            );
        }

        return verifyGroup(vos, session);
    }

    private List<VolumeBackupVO> verifyGroup(List<VolumeBackupVO> vos, SessionInventory session) {
        String groupUuid = vos.get(0).getUuid();
        long cnt = vos.stream().filter(vo -> vo.getType() == VolumeType.Root).count();
        if (cnt <= 0) {
            throw new OperationFailureException(
                    Platform.operr("Root volume missing within group uuid: %s", groupUuid)
            );
        }

        if (cnt > 1) {
            throw new OperationFailureException(
                    Platform.operr("Multiple root volumes found within group uuid: %s", groupUuid)
            );
        }

        if (!Account.isAdminPermission(session)) {
            List<String> backupUuids = vos.stream().map(ResourceVO::getUuid).collect(Collectors.toList());
            List<String> resUuids = AccessibleResourceChecker.forAccount(session.getAccountUuid())
                    .findOutAllInaccessibleResources(backupUuids);
            if (!resUuids.isEmpty()) {
                logger.warn(String.format("Account[%s] can not access volume backups: [%s]",
                        session.getAccountUuid(),
                        org.apache.commons.lang.StringUtils.join(resUuids, ",")));
                throw new OperationFailureException(
                        Platform.operr("No permission to volume backups within group uuid: %s", groupUuid)
                );
            }
        }

        return vos;
    }

    private void handle(APIRevertVmFromVmBackupMsg msg) {
        APIRevertVmFromVmBackupEvent evt = new APIRevertVmFromVmBackupEvent(msg.getId());

        List<VolumeBackupVO> vos = verifyGroupUuid(msg.getGroupUuid(), msg.getSession());
        Map<String, String> bsUuids = getBackupBsUuids(vos, msg.getBackupStrogeUuid());
        ErrorCodeList errList = new ErrorCodeList();

        new While<>(vos).each((vo, whileCompletion) -> {
            String backupUuid = vo.getUuid();
            String bsUuid = bsUuids.get(backupUuid);

            Completion comp = new Completion(whileCompletion) {
                @Override
                public void success() {
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    whileCompletion.allDone();
                }
            };

            if (vo.getType() == VolumeType.Root) {
                doRevertRootVolume(backupUuid, bsUuid, msg.getSession(), comp);
            } else {
                doRevertDataVolume(backupUuid, bsUuid, msg.getSession(), comp);
            }
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                vidm.revertExistingDeviceAddressFromArchive(msg.getVmInstanceUuid(), msg.getGroupUuid());

                if (!errList.getCauses().isEmpty()) {
                    evt.setError(errList.getCauses().get(0));
                }

                if (Objects.equals(msg.getStrategy(), VmCreationStrategy.InstantStart.toString())){
                    StartVmInstanceMsg smsg = new StartVmInstanceMsg();
                    smsg.setVmInstanceUuid(vos.get(0).getVmInstanceUuid());
                    bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, smsg.getVmInstanceUuid());
                    bus.send(smsg);
                }

                bus.publish(evt);
            }
        });
    }

    private void handle(APISyncVmBackupFromImageStoreBackupStorageMsg msg) {
        List<VolumeBackupVO> vos = verifyGroupUuid(msg.getGroupUuid(), msg.getSession());
        List<VolumeBackupInventory> res = new ArrayList<>();
        ErrorCodeList errList = new ErrorCodeList();

        new While<>(vos).each((vo, whileCompletion) -> {
            syncBackupInQueue(vo.getUuid(),
                    msg.getSrcBackupStorageUuid(),
                    msg.getDstBackupStorageUuid(),
                    new ReturnValueCompletion<VolumeBackupInventory>(msg, whileCompletion) {
                        @Override
                        public void success(VolumeBackupInventory returnValue) {
                            res.add(returnValue);
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            whileCompletion.allDone();
                        }
                    });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                APISyncVmBackupFromImageStoreBackupStorageEvent evt = new APISyncVmBackupFromImageStoreBackupStorageEvent(msg.getId());
                if (!errList.getCauses().isEmpty()) {
                    evt.setError(errList.getCauses().get(0));
                } else {
                    evt.setInventories(res);
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIRecoverVmBackupFromImageStoreBackupStorageMsg msg) {
        List<VolumeBackupVO> vos = verifyGroupUuid(msg.getGroupUuid(), msg.getSession());
        List<VolumeBackupInventory> res = new ArrayList<>();
        ErrorCodeList errList = new ErrorCodeList();

        new While<>(vos).each((vo, whileCompletion) -> {
            doRecoverBackup(vo.getUuid(),
                    msg.getSrcBackupStorageUuid(),
                    msg.getDstBackupStorageUuid(),
                    new ReturnValueCompletion<VolumeBackupInventory>(msg, whileCompletion) {
                        @Override
                        public void success(VolumeBackupInventory returnValue) {
                            res.add(returnValue);
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            whileCompletion.allDone();
                        }
                    });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                APIRecoverVmBackupFromImageStoreBackupStorageEvent evt = new APIRecoverVmBackupFromImageStoreBackupStorageEvent(msg.getId());
                if (!errList.getCauses().isEmpty()) {
                    evt.setError(errList.getCauses().get(0));
                } else {
                    evt.setInventories(res);
                }
                bus.publish(evt);
            }
        });
    }

    private Map<String, String> getBackupBsUuids(List<VolumeBackupVO> vos, String bsUuid) {
        Map<String, String> res = new HashMap<>();
        if (bsUuid != null) {
            vos.forEach(vo -> {
                res.put(vo.getUuid(), bsUuid);
            });

            return res;
        }

        List<String> remoteUuids = ImageStoreBackupStorageSelector.getRemoteBsUuids();
        vos.forEach(vo -> {
            List<String> backupStorageUuids = Q.New(VolumeBackupStorageRefVO.class)
                    .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, vo.getUuid())
                    .select(VolumeBackupStorageRefVO_.backupStorageUuid)
                    .listValues();
            backupStorageUuids.removeAll(remoteUuids);
            if (backupStorageUuids.isEmpty()) {
                throw new OperationFailureException(Platform.argerr("Volume backup[uuid:%s] not found on any backup storage", vo.getUuid()));
            }
            Collections.shuffle(backupStorageUuids);
            res.put(vo.getUuid(), backupStorageUuids.get(0));
        });

        return res;
    }

    private void doRevertDataVolume(String backupUuid,
                                    String backupStorageUuid,
                                    SessionInventory session,
                                    Completion completion) {
        VolumeVO volumeVO = SQL.New("select vol from VolumeVO vol, VolumeBackupVO bak "
                + "where bak.uuid = :backupUuid and vol.uuid = bak.volumeUuid", VolumeVO.class)
                .param("backupUuid", backupUuid)
                .find();
        VolumeInventory volume = VolumeInventory.valueOf(volumeVO);

        // 1. Create data volume
        // 2. Instantiate data volume
        // 3. Delete old data volume on PS
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("revert-data-volume-from-backup-" + backupStorageUuid);
        chain.then(new ShareFlow() {
            String imageUuid;
            VolumeInventory transientVolume;
            String hostUuid;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        TemplateCreateStruct struct = makeCreateTemplateStruct(backupUuid, backupStorageUuid, session);
                        struct.setName("for-recover-data-volume-from-backup-" + backupUuid);

                        doCreateTemplateFromBackup(struct, ImageConstant.ImageMediaType.DataVolumeTemplate, new ReturnValueCompletion<ImageInventory>(trigger) {
                            @Override
                            public void success(ImageInventory inventory) {
                                imageUuid = inventory.getUuid();
                                tagMgr.createNonInherentSystemTag(imageUuid,
                                        ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                        ImageVO.class.getSimpleName());

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
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetPrimaryStorageResourceLocationMsg msg = new GetPrimaryStorageResourceLocationMsg();
                        msg.setResourceUuid(volumeVO.getUuid());
                        msg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                        msg.setResourceType(VolumeVO.class.getSimpleName());
                        bus.makeLocalServiceId(msg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    GetPrimaryStorageResourceLocationReply r = reply.castReply();
                                    if (r.getHostUuids() != null && !r.getHostUuids().isEmpty()) {
                                        hostUuid = r.getHostUuids().get(new Random().nextInt(r.getHostUuids().size()));
                                    }
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateTemporaryDataVolumeFromVolumeTemplateMsg cmsg = new CreateTemporaryDataVolumeFromVolumeTemplateMsg();
                        cmsg.setOriginVolumeUuid(volume.getUuid());
                        cmsg.setImageUuid(imageUuid);
                        cmsg.setName(volumeVO.getName());
                        cmsg.setDescription("Recovered from backup: " + backupUuid);
                        cmsg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());

                        if (VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(volumeVO.getUuid(), VolumeVO.class)) {
                            cmsg.setSystemTags(Arrays.asList(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.getTag(volumeVO.getUuid(), VolumeVO.class)));
                        }

                        if (volumeVO.getVmInstanceUuid() != null) {
                            VmInstanceVO vmInstanceVO = dbf.findByUuid(volumeVO.getVmInstanceUuid(), VmInstanceVO.class);
                            cmsg.setHostUuid(vmInstanceVO.getHostUuid() == null ? vmInstanceVO.getLastHostUuid() : vmInstanceVO.getHostUuid());
                        } else {
                            cmsg.setHostUuid(hostUuid);
                        }

                        cmsg.setAccountUuid(session.getAccountUuid());
                        bus.makeLocalServiceId(cmsg, VolumeConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateDataVolumeFromVolumeTemplateReply r = reply.castReply();
                                    transientVolume = r.getInventory();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        deleteImageAsync(imageUuid, null);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public boolean skip(Map data) {
                        return volume.getVmInstanceUuid() == null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                        amsg.setVolume(transientVolume);
                        amsg.setVmInstanceUuid(volume.getVmInstanceUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                        bus.send(amsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        OverwriteVolumeMsg amsg = new OverwriteVolumeMsg();
                        amsg.setOriginVolume(volume);
                        amsg.setTransientVolume(transientVolume);
                        bus.makeTargetServiceIdByResourceUuid(amsg, VolumeConstant.SERVICE_ID, amsg.getVolumeUuid());
                        bus.send(amsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                if (MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class)) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-temp-image";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            deleteImageAsync(imageUuid, null);
                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();

    }

    private void doRevertRootVolume(String backupUuid,
                                    String backupStorageUuid,
                                    SessionInventory session,
                                    Completion completion) {
        // 1. Create root volume image from backup
        // 2. change vm image
        // 3. remove old volume? No.
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("revert-root-volume-from-backup-" + backupStorageUuid);
        chain.then(new ShareFlow() {
            String imageUuid;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        TemplateCreateStruct struct = makeCreateTemplateStruct(backupUuid, backupStorageUuid, session);
                        struct.setName("for-recover-volume-from-backup-" + backupUuid);

                        doCreateTemplateFromBackup(struct, ImageConstant.ImageMediaType.RootVolumeTemplate, new ReturnValueCompletion<ImageInventory>(trigger) {
                            @Override
                            public void success(ImageInventory inventory) {
                                imageUuid = inventory.getUuid();
                                tagMgr.createNonInherentSystemTag(imageUuid,
                                        ImageSystemTags.IMAGE_CREATED_BY_SYSTEM.getTagFormat(),
                                        ImageVO.class.getSimpleName());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ChangeVmImageMsg cmsg = new ChangeVmImageMsg();
                        cmsg.setImageUuid(imageUuid);
                        cmsg.setVmInstanceUuid(getVmInstanceUuid(backupUuid));
                        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        deleteImageAsync(imageUuid, null);
                        trigger.rollback();
                    }
                });

                if (MevocoGlobalConfig.DELETE_TEMP_IMAGES.value(Boolean.class)) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-temp-image";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            deleteImageAsync(imageUuid, null);
                            trigger.next();
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VolumeBackupConstant.SERVICE_ID);
    }

    private void populateClusterFactories() {
        for (VolumeBackupFactory ext : pluginRgty.getExtensionList(VolumeBackupFactory.class)) {
            VolumeBackupFactory old = bkFactories.get(ext.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VolumeBackupFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHypervisorType()));
            }

            bkFactories.put(ext.getHypervisorType(), ext);
        }
    }

    private void initSystemTags() {
        SystemTagLifeCycleListener listener = new SystemTagLifeCycleListener() {
            private final List<Class<? extends SchedulerJob>> jobs = Arrays.asList(CreateVmBackupJob.class,
                    CreateRootVolumeBackupJob.class, CreateVolumeBackupJob.class, CreateDatabaseBackupJob.class);

            @Override
            public void tagCreated(SystemTagInventory tag) {

            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                deleteBackup(tag.getResourceUuid());
                SchedulerJobParamCascadeUpdater.updateJobForResourceDisabled(tag.getResourceUuid(), BackupStorageVO.class.getSimpleName(), jobs);
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }

            private void deleteBackup(String bsUuid) {
                Map<String, String> volumeBackupAndVolumeUuids = SQL.New("select ref.volumeBackupUuid, backup.volumeUuid" +
                        " from VolumeBackupStorageRefVO ref, VolumeBackupVO backup" +
                        " where ref.volumeBackupUuid = backup.uuid" +
                        " and ref.backupStorageUuid = :bsUuid", Tuple.class)
                        .param("bsUuid", bsUuid)
                        .list().stream().collect(Collectors.toMap(
                                t -> ((Tuple)t).get(0, String.class), t -> ((Tuple)t).get(1, String.class))
                        );

                logger.debug(String.format("image store[uuid: %s] was no longer used for backup, delete %d volume backup(s) on it",
                        bsUuid, volumeBackupAndVolumeUuids.size()));

                new While<>(volumeBackupAndVolumeUuids.entrySet()).step((e, compl) -> {
                    VolumeBackupDeletionMsg msg = new VolumeBackupDeletionMsg();
                    msg.setUuid(e.getKey());
                    msg.setBackupStorageUuids(Collections.singletonList(bsUuid));
                    msg.setDbOnly(true);
                    bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, e.getValue());
                    bus.send(msg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply reply) {
                            compl.done();
                        }
                    });
                }, 10).run(new NopeWhileDoneCompletion());
            }
        };
        BackupHelper.installLifeCycleListener(listener);
    }

    @Override
    public boolean start() {
        populateClusterFactories();
        installValidatorToSystemTag();
        initSystemTags();

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (msg instanceof NeedQuotaCheckMessage) {
                    if (((NeedQuotaCheckMessage) msg).getAccountUuid() == null ||
                            ((NeedQuotaCheckMessage) msg).getAccountUuid().equals("")) {
                        // skip admin scheduler
                        return;
                    }
                    List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
                    if (quotas == null || quotas.size() == 0) {
                        return;
                    }
                    Map<String, Quota.QuotaPair> pairs = new QuotaUtil().
                            makeQuotaPairs(((NeedQuotaCheckMessage) msg).getAccountUuid());
                    for (Quota quota : quotas) {
                        quota.getOperator().checkQuota((NeedQuotaCheckMessage) msg, pairs);
                    }
                }
            }
        }, CreateVmBackupMsg.class, CreateVolumeBackupMsg.class);

        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        changeVolumeBackupOwner(ref, newOwnerUuid);
    }

    private void installValidatorToSystemTag() {
        ImageStoreSystemTags.BACKUP_CIDR.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String cidr = ImageStoreSystemTags.BACKUP_CIDR.getTokenByTag(systemTag,
                        ImageStoreSystemTags.BACKUP_CIDR_TOKEN);
                String fmtCidr = NetworkUtils.fmtCidr(cidr);
                if (!fmtCidr.equals(cidr)) {
                    throw new OperationFailureException(argerr("[%s] is not a standard cidr, do you mean [%s]?", cidr, fmtCidr));
                }
            }
        });

        class ParallelismValidator implements SystemTagValidator, SystemTagCreateMessageValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg instanceof APIAddBackupStorageMsg) {
                    if (msg.getSystemTags() == null) {
                        return;
                    }

                    msg.getSystemTags().stream().filter(it -> VolumeBackupSystemTag.LIVE_BACKUP_PARALLELISM.isMatch(it))
                            .findFirst().ifPresent(this::validate);
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                validate(systemTag);
            }

            private void validate(String tag) {
                String degree = VolumeBackupSystemTag.LIVE_BACKUP_PARALLELISM.getTokenByTag(tag,
                        VolumeBackupSystemTag.DEGREE);
                if (!NumberUtils.isNumber(degree) || Integer.parseInt(degree) <= 0) {
                    throw new OperationFailureException(argerr("degree [%s] should be a positive number", degree));
                }
            }
        }

        ParallelismValidator parallelismValidator = new ParallelismValidator();
        VolumeBackupSystemTag.LIVE_BACKUP_PARALLELISM.installValidator(parallelismValidator);
        tagMgr.installCreateMessageValidator(BackupStorageVO.class.getSimpleName(), parallelismValidator);

        class StorageInfoValidator implements SystemTagValidator, SystemTagCreateMessageValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg instanceof APIAddBackupStorageMsg) {
                    if (msg.getSystemTags() == null) {
                        return;
                    }

                    msg.getSystemTags().stream().filter(it -> ImageStoreSystemTags.STORAGE_INFO.isMatch(it))
                            .findFirst().ifPresent(this::validate);
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                validate(systemTag);
            }

            private void validate(String tag) {
                Map<String, String> infos = ImageStoreSystemTags.STORAGE_INFO.getTokensByTag(tag);
                String type = infos.get(ImageStoreSystemTags.FS_TYPE_TOKEN);
                String url = infos.get(ImageStoreSystemTags.URL_TOKEN);
                String options = infos.get(ImageStoreSystemTags.OPTION_TOKEN);

                if (!Arrays.asList("nfs", "sshfs", "nbd").contains(type)) {
                    throw new OperationFailureException(argerr("invalid type[%s], should be [nfs, sshfs, nbd]", type));
                }

                if (!type.equals("nfs")) {
                    return;
                }

                try {
                    URI uri = new URI("nfs://" +  url);
                    if (Strings.isEmpty(uri.getPath()) || Strings.isEmpty(uri.getHost())) {
                        throw new OperationFailureException(argerr("invalid url[%s], should be hostname:/path", url));
                    }
                } catch (URISyntaxException e) {
                    throw new OperationFailureException(argerr("invalid url[%s], should be hostname:/path", url));
                }
            }
        }

        StorageInfoValidator storageInfoValidator = new StorageInfoValidator();
        ImageStoreSystemTags.STORAGE_INFO.installValidator(storageInfoValidator);
        tagMgr.installCreateMessageValidator(BackupStorageVO.class.getSimpleName(), storageInfoValidator);
    }

    private void changeVolumeBackupOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        List<String> bkUuids = Q.New(VolumeBackupVO.class)
                .select(VolumeBackupVO_.uuid)
                .eq(VolumeBackupVO_.volumeUuid, ref.getResourceUuid())
                .listValues();

        for (String spUuid : bkUuids) {
            acntMgr.changeResourceOwner(spUuid, newOwnerUuid);
        }
    }

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType vmMigrationType, String dstHostUuid, Completion completion) {
        if (!VmMigrationType.HostMigration.equals(vmMigrationType)) {
            completion.success();
            return;
        }

        deleteVmBackupRecords(vm);
        completion.success();
    }

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        deleteVmBackupRecords(spec.getVmInventory());
    }

    @ExceptionSafe
    private void deleteVmBackupRecords(VmInstanceInventory vm) {
        List<String> volumeUuids = vm.getAllVolumes().stream()
                .map(VolumeInventory::getUuid)
                .collect(Collectors.toList());
        SQL.New(VolumeBackupHistoryVO.class)
                .in(VolumeBackupHistoryVO_.uuid, volumeUuids)
                .delete();
    }

    @Override
    public List<String> getAvailableImageInstallPaths(String backupStorageUuid) {
        return Q.New(VolumeBackupStorageRefVO.class).select(VolumeBackupStorageRefVO_.installPath)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, backupStorageUuid)
                .listValues();
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new VolumeBackupNumQuotaDefinition());
        quota.defineQuota(new VolumeBackupSizeQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVmBackupMsg.class)
                .addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM, (msg) -> {
                    VmInstanceVO vmVO = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid())
                            .find();
                    return (long) vmVO.getAllDiskVolumes().size();
                }).addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE, (msg) -> {
                    VmInstanceVO vmVO = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid())
                            .find();
                    long requestCapacity;
                    if (BackupMode.full.toString().equals(msg.getMode())) {
                        requestCapacity = vmVO.getAllDiskVolumes().stream().mapToLong(VolumeVO::getSize).sum();
                    } else {
                        //TODO: predict the size of a backup
                        requestCapacity = 1;
                    }

                    return requestCapacity;
                }));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVolumeBackupMsg.class)
                .addFixedRequiredSize(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM, 1L)
                .addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE, (msg) -> {
                    long requestCapacity;
                    if (BackupMode.full.toString().equals(msg.getMode())) {
                        requestCapacity = Q.New(VolumeVO.class)
                                .select(VolumeVO_.size)
                                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                                .findValue();
                    } else {
                        //TODO: predict the size of a backup
                        requestCapacity = 1;
                    }

                    return requestCapacity;
                }));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(CreateVmBackupMsg.class)
                .addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM, (msg) -> {
                    VmInstanceVO vmVO = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid())
                            .find();
                    return (long) vmVO.getAllDiskVolumes().size();
                }).addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE, (msg) -> {
                    VmInstanceVO vmVO = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid())
                            .find();
                    long requestCapacity;
                    if (BackupMode.full == msg.getMode()) {
                        requestCapacity = vmVO.getAllDiskVolumes().stream().mapToLong(VolumeVO::getSize).sum();
                    } else {
                        //TODO: predict the size of a backup
                        requestCapacity = 1;
                    }

                    return requestCapacity;
                }));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(CreateVolumeBackupMsg.class)
                .addFixedRequiredSize(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM, 1L)
                .addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE, (msg) -> {
                    long requestCapacity;
                    if (BackupMode.full == msg.getMode()) {
                        requestCapacity = Q.New(VolumeVO.class)
                                .select(VolumeVO_.size)
                                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                                .findValue();
                    } else {
                        //TODO: predict the size of a backup
                        requestCapacity = 1;
                    }

                    return requestCapacity;
                }));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateRootVolumeTemplateFromVolumeBackupMsg.class)
                .addMessageRequiredQuotaHandler(ImageQuotaConstant.IMAGE_SIZE, (msg) ->
                        getCreateTemplateFromVolumeBackupRequiredImageSize(Collections.singletonList(msg.getBackupUuid())))
                .addCounterQuota(ImageQuotaConstant.IMAGE_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateDataVolumeTemplateFromVolumeBackupMsg.class)
                .addMessageRequiredQuotaHandler(ImageQuotaConstant.IMAGE_SIZE, (msg) ->
                        getCreateTemplateFromVolumeBackupRequiredImageSize(Collections.singletonList(msg.getBackupUuid())))
                .addCounterQuota(ImageQuotaConstant.IMAGE_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVmFromVmBackupMsg.class)
                .addMessageRequiredQuotaHandler(ImageQuotaConstant.IMAGE_SIZE, (msg) -> {
                    List<String> volumeBackupUuids = Q.New(VolumeBackupVO.class)
                            .select(VolumeBackupVO_.uuid)
                            .eq(VolumeBackupVO_.groupUuid, msg.getGroupUuid())
                            .listValues();
                    return getCreateTemplateFromVolumeBackupRequiredImageSize(volumeBackupUuids);
                }).addMessageRequiredQuotaHandler(ImageQuotaConstant.IMAGE_NUM, (msg) -> Q.New(VolumeBackupVO.class)
                        .select(VolumeBackupVO_.uuid)
                        .eq(VolumeBackupVO_.groupUuid, msg.getGroupUuid())
                        .count()));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(VolumeBackupVO.class)
                        .eq(VolumeBackupVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM)
                .addMessageRequiredQuotaHandler(VolumeBackupQuotaConstant.VOLUME_BACKUP_SIZE, (msg) -> Q.New(VolumeBackupVO.class)
                        .select(VolumeBackupVO_.size)
                        .eq(VolumeBackupVO_.uuid, msg.getResourceUuid())
                        .findValue()));

        return list(quota);
    }

    @Transactional(readOnly = true)
    private Long getCreateTemplateFromVolumeBackupRequiredImageSize(List<String> volumeBackupUuids) {
        List<String> backupMeta = Q.New(VolumeBackupVO.class)
                .select(VolumeBackupVO_.metadata)
                .in(VolumeBackupVO_.uuid, volumeBackupUuids).listValues();
        long requiredImageSize = 0L;

        for (String meta : backupMeta) {
            if (meta == null) {
                continue;
            }

            VolumeMetadata metadata = VolumeMetaDataMaker.parseVolumeMetadata(meta);
            requiredImageSize += metadata.getSize();
        }

        return requiredImageSize;
    }
}
