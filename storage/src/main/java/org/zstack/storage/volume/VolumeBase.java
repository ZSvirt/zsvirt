package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.message.APIDeleteMessage.DeletionMode;
import org.zstack.header.message.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.*;
import org.zstack.header.tpm.TpmConstants;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.volume.*;
import org.zstack.header.volume.VolumeConstant.Capability;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.AccountManager;
import org.zstack.header.tpm.message.BackupTpmEncryptionKeyMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.snapshot.VolumeSnapshotGroupSystemTags;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupOperationValidator;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.storage.volume.VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY;
import static org.zstack.storage.volume.VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:20 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBase extends AbstractVolume implements Volume {
    private static final CLogger logger = Utils.getLogger(VolumeBase.class);
    protected String syncThreadId;
    protected VolumeVO self;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VolumeDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private StorageTrash trash;

    public VolumeBase(VolumeVO vo) {
        self = vo;
        syncThreadId = String.format("volume-%s", self.getUuid());
    }

    protected void refreshVO() {
        self = dbf.reload(self);
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VolumeDeletionMsg) {
            handle((VolumeDeletionMsg) msg);
        } else if (msg instanceof DeleteVolumeMsg) {
            handle((DeleteVolumeMsg) msg);
        } else if (msg instanceof VolumeCreateSnapshotMsg) {
            handle((VolumeCreateSnapshotMsg) msg);
        } else if (msg instanceof CreateImageCacheFromVolumeMsg) {
            handle((CreateImageCacheFromVolumeMsg) msg);
        } else if (msg instanceof CreateDataVolumeTemplateFromDataVolumeMsg) {
            handle((CreateDataVolumeTemplateFromDataVolumeMsg) msg);
        } else if (msg instanceof ExpungeVolumeMsg) {
            handle((ExpungeVolumeMsg) msg);
        } else if (msg instanceof RecoverVolumeMsg) {
            handle((RecoverVolumeMsg) msg);
        } else if (msg instanceof EstimateVolumeTemplateSizeMsg) {
            handle((EstimateVolumeTemplateSizeMsg) msg);
        }  else if (msg instanceof SyncVolumeSizeMsg) {
            handle((SyncVolumeSizeMsg) msg);
        } else if (msg instanceof InstantiateVolumeMsg) {
            handle((InstantiateVolumeMsg) msg);
        } else if (msg instanceof OverlayMessage) {
            handle((OverlayMessage) msg);
        } else if (msg instanceof MulitpleOverlayMsg) {
            handle((MulitpleOverlayMsg) msg);
        } else if (msg instanceof ChangeVolumeStatusMsg) {
            handle((ChangeVolumeStatusMsg) msg);
        } else if (msg instanceof OverwriteVolumeMsg) {
            handle((OverwriteVolumeMsg) msg);
        } else if (msg instanceof ReInitVolumeMsg) {
            handle((ReInitVolumeMsg) msg);
        } else if (msg instanceof GetVolumeBackingInstallPathMsg) {
            handle((GetVolumeBackingInstallPathMsg) msg);
        } else if (msg instanceof SetVmBootVolumeMsg) {
            handle((SetVmBootVolumeMsg) msg);
        } else if (msg instanceof ChangeVolumeTypeMsg) {
            handle((ChangeVolumeTypeMsg) msg);
        } else if (msg instanceof CreateVolumeSnapshotGroupMsg) {
            handle((CreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof FlattenVolumeMsg) {
            handle((FlattenVolumeMsg) msg);
        } else if (msg instanceof CancelFlattenVolumeMsg) {
            handle((CancelFlattenVolumeMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ReInitVolumeMsg msg) {
        ReInitVolumeReply reply = new ReInitVolumeReply();
        refreshVO();

        VolumeInventory rootVolumeInventory = VolumeInventory.valueOf(self);
        final long originSize = Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.size)
                .limit(1)
                .eq(ImageCacheVO_.imageUuid, self.getRootImageUuid())
                .findValue();
        // do the re-image op
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        List<String> systemTags = Q.New(SystemTagVO.class).select(SystemTagVO_.tag)
                .eq(SystemTagVO_.resourceUuid, rootVolumeInventory.getUuid())
                .listValues();
        chain.setName(String.format("reset-root-volume-%s-from-image-%s", self.getUuid(), self.getRootImageUuid()));
        chain.then(new ShareFlow() {
            VolumeVO vo = self;
            String allocatedInstallUrl;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-template-available";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        boolean cacheExists = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, self.getRootImageUuid())
                                .eq(ImageCacheVO_.primaryStorageUuid, self.getPrimaryStorageUuid())
                                .isExists();

                        boolean imageExists = Q.New(ImageVO.class).eq(ImageVO_.uuid, self.getRootImageUuid()).isExists();
                        if (!cacheExists && !imageExists) {
                            trigger.fail(operr("cannot find image cache[imageUuid: %s] for reinit volume", self.getRootImageUuid()));
                            return;
                        }

                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    boolean success;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateRootVolume.toString());
                        amsg.setSize(originSize);
                        amsg.setRequiredHostUuid(msg.getHostUuid());
                        amsg.setSystemTags(systemTags);
                        amsg.setRequiredInstallUri(String.format("volume://%s", vo.getUuid()));

                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstallUrl = ar.getAllocatedInstallUrl();
                                success = true;
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            rmsg.setDiskSize(originSize);
                            rmsg.setAllocatedInstallUrl(allocatedInstallUrl);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(rmsg);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "reset-root-volume-from-image-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ReInitRootVolumeFromTemplateOnPrimaryStorageMsg rmsg = new ReInitRootVolumeFromTemplateOnPrimaryStorageMsg();
                        rmsg.setVolume(rootVolumeInventory);
                        rmsg.setOriginSize(originSize);
                        rmsg.setAllocatedInstallUrl(allocatedInstallUrl);
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rootVolumeInventory.getPrimaryStorageUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    ReInitRootVolumeFromTemplateOnPrimaryStorageReply re = (ReInitRootVolumeFromTemplateOnPrimaryStorageReply) reply;
                                    vo.setInstallPath(re.getNewVolumeInstallPath());
                                    vo = dbf.updateAndRefresh(vo);
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-volume-size-after-reimage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                        smsg.setVolumeUuid(vo.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VolumeConstant.SERVICE_ID, rootVolumeInventory.getUuid());
                        bus.send(smsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.debug(String.format("failed to sync volume size after reimage, %s", reply.getError()));
                                    vo.setSize(originSize);
                                }
                                vo.setSize(((SyncVolumeSizeReply) reply).getSize());
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "move-origin-volume-to-trash";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        trash.createTrash(TrashType.ReimageVolume, false, rootVolumeInventory);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        dbf.update(vo);

                        List<AfterReimageVmInstanceExtensionPoint> list = pluginRgty.getExtensionList(
                                AfterReimageVmInstanceExtensionPoint.class);
                        for (AfterReimageVmInstanceExtensionPoint ext : list) {
                            ext.afterReimageVmInstance(rootVolumeInventory);
                        }

                        self = dbf.reload(self);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to restore volume[uuid:%s] to image[uuid:%s], %s",
                                rootVolumeInventory.getUuid(), rootVolumeInventory.getRootImageUuid(), errCode));
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(ChangeVolumeStatusMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            @Deferred
            public void run(SyncTaskChain chain) {
                refreshVO();

                Defer.defer(() -> {
                    ChangeVolumeStatusReply reply = new ChangeVolumeStatusReply();
                    bus.reply(msg, reply);
                });

                if (self == null) {
                    // volume has been deleted by previous request
                    // this happens when delete vm request queued before
                    // migrating trigger not by api
                    // in this case, ignore change state request
                    logger.debug(String.format("volume[uuid:%s] has been deleted, ignore change volume state request",
                            msg.getVolumeUuid()));
                    chain.next();
                    return;
                }

                VolumeStatus bs = self.getStatus();
                SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).set(VolumeVO_.status, msg.getStatus()).update();
                refreshVO();
                logger.debug(String.format("volume[uuid:%s] status changed from %s to %s in db", self.getUuid(), bs, self.getStatus()));
                chain.next();
            }

            @Override
            public String getName() {
                return "change-volume-status";
            }
        });
    }

    private void handle(InstantiateVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("instantiate-volume-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doInstantiateVolume(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "instantiate-volume";
            }
        });
    }

    private void doInstantiateVolume(InstantiateVolumeMsg msg, NoErrorCompletion completion) {
        InstantiateVolumeReply reply = new InstantiateVolumeReply();

        // InstantiateVolumeMsg is queued, but we need to check whether the volume has been instantiated
        self = dbf.reload(self);
        if (self.getStatus() == VolumeStatus.Ready) {
            reply.setVolume(getSelfInventory());
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        List<PreInstantiateVolumeExtensionPoint> exts = pluginRgty.getExtensionList(PreInstantiateVolumeExtensionPoint.class);
        for (PreInstantiateVolumeExtensionPoint ext : exts) {
            ext.preInstantiateVolume(msg);
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-volume-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            String installPath;
            String format;
            String protocol;
            Long actualSize;
            Long virtualSize;

            @Override
            public void setup() {
                if (!msg.isPrimaryStorageAllocated()) {
                    flow(new Flow() {
                        String __name__ = "allocate-primary-storage";

                        boolean success;
                        String allocateInstallUrl;

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                            amsg.setSystemTags(msg.getSystemTags());
                            amsg.setRequiredHostUuid(msg.getHostUuid());
                            amsg.setRequiredPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                            amsg.setSize(self.getSize());
                            if (self.getType() == VolumeType.Root) {
                                amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateRootVolume.toString());
                            } else {
                                amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
                            }
                            amsg.setDiskOfferingUuid(self.getDiskOfferingUuid());

                            if (msg.getPrimaryStorageUuid() != null) {
                                bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                            } else {
                                bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                            }
                            bus.send(amsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                    } else {
                                        AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                        if (msg.getPrimaryStorageUuid() == null) {
                                            msg.setPrimaryStorageUuid(ar.getPrimaryStorageInventory().getUuid());
                                        }
                                        // set the primary storage uuid in advance to make the leastVolumeFirstAllocateStrategy allocation more balanced
                                        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, self.getUuid())
                                                .set(VolumeVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                                                .update();
                                        success = true;
                                        allocateInstallUrl = ar.getAllocatedInstallUrl();
                                        msg.setAllocatedInstallUrl(allocateInstallUrl);
                                        trigger.next();
                                    }
                                }
                            });
                        }

                        @Override
                        public void rollback(FlowRollback trigger, Map data) {
                            if (success) {
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setAllocatedInstallUrl(allocateInstallUrl);
                                rmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                                rmsg.setDiskSize(self.getSize());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                                bus.send(rmsg);
                                SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, self.getUuid())
                                        .set(VolumeVO_.primaryStorageUuid, null)
                                        .update();
                            }

                            trigger.rollback();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "instantiate-volume-on-primary-storage";

                    boolean success;

                    boolean incremental;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String psType = Q.New(PrimaryStorageVO.class)
                                .select(PrimaryStorageVO_.type)
                                .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                                .findValue();

                        InstantiateDataVolumeOnCreationExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, InstantiateDataVolumeOnCreationExtensionPoint.class);
                        if (ext != null) {
                            ext.instantiateDataVolumeOnCreation(msg, getSelfInventory(), new ReturnValueCompletion<VolumeInventory>(trigger) {
                                @Override
                                public void success(VolumeInventory ret) {
                                    refreshVO();
                                    success = true;
                                    installPath = ret.getInstallPath();
                                    format = ret.getFormat();
                                    actualSize = ret.getActualSize();
                                    virtualSize = ret.getSize();

                                    buildSnapshotRefFromTemplateIfNeed(ret);

                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        } else {
                            if (msg instanceof InstantiateTemporaryRootVolumeMsg) {
                                instantiateTemporaryRootVolume((InstantiateTemporaryRootVolumeMsg) msg , trigger);
                            } else if (msg instanceof InstantiateRootVolumeMsg) {
                                instantiateRootVolume((InstantiateRootVolumeMsg) msg, trigger);
                            } else if (msg instanceof InstantiateMemoryVolumeMsg) {
                                instantiateMemoryVolume(msg, trigger);
                            } else {
                                // include: data volume, NvRam, TpmState
                                instantiateOtherVolume(msg, trigger);
                            }
                        }
                    }

                    private void buildSnapshotRefFromTemplateIfNeed(VolumeInventory inv) {
                        if (msg instanceof InstantiateRootVolumeMsg &&
                                ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(((InstantiateRootVolumeMsg) msg).getTemplateSpec().getInventory().getMediaType())) {
                            incremental = VolumeSnapshotReferenceUtils.buildSnapshotReferenceForNewVolumeIfNeed(
                                     inv, ((InstantiateRootVolumeMsg) msg).getTemplateSpec().getInventory().getUuid()
                            ) != null;
                        }
                    }

                    private void instantiateMemoryVolume(InstantiateVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateMemoryVolumeOnPrimaryStorageMsg imsg = new InstantiateMemoryVolumeOnPrimaryStorageMsg();
                        prepareMsg(msg, imsg);
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void instantiateTemporaryRootVolume(InstantiateTemporaryRootVolumeMsg msg , FlowTrigger trigger) {
                        InstantiateVolumeOnPrimaryStorageMsg imsg;
                        if (ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(msg.getTemplateSpec().getInventory().getMediaType())) {
                            imsg = new InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg();
                            ((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg)imsg).setOriginVolumeUuid(msg.getOriginVolumeUuid());
                            ((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg)imsg).setTemplateSpec(msg.getTemplateSpec());
                        } else {
                            imsg = new InstantiateTemporaryVolumeOnPrimaryStorageMsg();
                            ((InstantiateTemporaryVolumeOnPrimaryStorageMsg)imsg).setOriginVolumeUuid(msg.getOriginVolumeUuid());
                        }

                        prepareMsg(msg, imsg);
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void instantiateRootVolume(InstantiateRootVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg imsg = new InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg();
                        prepareMsg(msg, imsg);
                        imsg.setTemplateSpec(msg.getTemplateSpec());
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void instantiateOtherVolume(InstantiateVolumeMsg msg, FlowTrigger trigger) {
                        InstantiateVolumeOnPrimaryStorageMsg imsg = new InstantiateVolumeOnPrimaryStorageMsg();
                        prepareMsg(msg, imsg);
                        doInstantiateVolume(imsg, trigger);
                    }

                    private void prepareMsg(InstantiateVolumeMsg msg, InstantiateVolumeOnPrimaryStorageMsg imsg) {
                        imsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        imsg.setVolume(getSelfInventory());
                        imsg.setSystemTags(msg.getSystemTags());
                        imsg.setSkipIfExisting(msg.isSkipIfExisting());
                        imsg.setAllocatedInstallUrl(msg.getAllocatedInstallUrl());
                        if (msg.getHostUuid() != null) {
                            imsg.setDestHost(HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class)));
                        }
                        bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                    }

                    private void doInstantiateVolume(InstantiateVolumeOnPrimaryStorageMsg imsg, FlowTrigger trigger) {
                        bus.send(imsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                refreshVO();
                                success = true;
                                InstantiateVolumeOnPrimaryStorageReply ir = reply.castReply();
                                installPath = ir.getVolume().getInstallPath();
                                format = ir.getVolume().getFormat();
                                actualSize = ir.getVolume().getActualSize();
                                virtualSize = ir.getVolume().getSize();
                                protocol = ir.getVolume().getProtocol();

                                List<AfterInstantiateVolumeExtensionPoint> exts = pluginRgty.getExtensionList(AfterInstantiateVolumeExtensionPoint.class);
                                for (AfterInstantiateVolumeExtensionPoint ext : exts) {
                                    ext.afterInstantiateVolume(imsg);
                                }

                                buildSnapshotRefFromTemplateIfNeed(ir.getVolume());

                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                            dmsg.setUuid(msg.getPrimaryStorageUuid());
                            dmsg.setVolume(getSelfInventory());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        }

                        if (incremental) {
                            VolumeSnapshotReferenceUtils.rollbackSnapshotReferenceForNewVolume(self.getUuid());
                        }

                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        VolumeStatus oldStatus = self.getStatus();
                        self.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                        self.setInstallPath(installPath);
                        DebugUtils.Assert(format != null, "format cannot be null");
                        self.setFormat(format);
                        self.setStatus(VolumeStatus.Ready);
                        if (actualSize != null) {
                            self.setActualSize(actualSize);
                        }
                        if (virtualSize != 0 && virtualSize != self.getSize()) {
                            self.setSize(virtualSize);
                        }
                        if (protocol != null) {
                            self.setProtocol(protocol);
                        }
                        self = dbf.updateAndRefresh(self);

                        VolumeInventory vol = getSelfInventory();
                        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, vol);

                        reply.setVolume(vol);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                Finally(new FlowFinallyHandler(msg, completion) {
                    @Override
                    public void Finally() {
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(OverlayMessage msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                if (msg instanceof VolumeTemplateOverlayMsg) {
                    if (PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class) &&
                            !msg.getMessage().hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat())) {
                        return syncThreadId;
                    }

                    return String.format("create-volume-template-%s", self.getUuid());
                }

                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                ErrorCode err = validateOperationByState(msg, self.getStatus(), SysErrors.OPERATION_ERROR);
                if (err != null) {
                    throw new OperationFailureException(err);
                }

                doOverlayMessage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getTaskName();
            }
        });
    }

    private void doOverlayMessage(OverlayMessage msg, NoErrorCompletion noErrorCompletion) {
        bus.send(msg.getMessage(), new CloudBusCallBack(msg, noErrorCompletion) {
            @Override
            public void run(MessageReply reply) {
                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void handle(final MulitpleOverlayMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                ErrorCode err = validateOperationByState(msg, self.getStatus(), SysErrors.OPERATION_ERROR);
                if (err != null) {
                    throw new OperationFailureException(err);
                }

                bus.send(msg.getMessages(), new CloudBusListCallBack(msg, chain) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        MulitpleOverlayReply reply = new MulitpleOverlayReply();
                        reply.setInnerReplies(replies);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "multiple-overlay-message";
            }
        });
    }

    private void handle(final SyncVolumeSizeMsg msg) {
        final SyncVolumeSizeReply reply = new SyncVolumeSizeReply();
        syncVolumeVolumeSize(new ReturnValueCompletion<VolumeSize>(msg) {
            @Override
            public void success(VolumeSize ret) {
                reply.setActualSize(ret.actualSize);
                reply.setSize(ret.size);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final EstimateVolumeTemplateSizeMsg msg) {
        final EstimateVolumeTemplateSizeReply reply = new EstimateVolumeTemplateSizeReply();

        EstimateVolumeTemplateSizeOnPrimaryStorageMsg emsg = new EstimateVolumeTemplateSizeOnPrimaryStorageMsg();
        emsg.setVolumeUuid(msg.getVolumeUuid());
        emsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
        emsg.setInstallPath(self.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(emsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(emsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (r.isSuccess()) {
                    EstimateVolumeTemplateSizeOnPrimaryStorageReply er = r.castReply();
                    reply.setActualSize(er.getActualSize());
                    reply.setSize(self.getSize());
                    reply.setWithInternalSnapshot(er.isWithInternalSnapshot());
                    bus.reply(msg, reply);
                    return;
                }

                if (msg.isIgnoreError()) {
                    logger.warn(String.format("failed to estimate size of the volume[uuid:%s] on the primary storage[uuid:%s], %s," +
                            " however, as the message[ignoreError] is set, use actual size in db instead.", self.getUuid(), self.getPrimaryStorageUuid(), r.getError()));
                    reply.setSize(self.getSize());
                    reply.setActualSize(self.getActualSize());
                    bus.reply(msg, reply);
                    return;
                }

                reply.setError(r.getError());
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final RecoverVolumeMsg msg) {
        final RecoverVolumeReply reply = new RecoverVolumeReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                recoverVolume(new Completion(chain, msg) {
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
                return RecoverVolumeMsg.class.getName();
            }
        });
    }

    private void expunge(final Completion completion) {
        if (self == null) {
            completion.success();
            return;
        }

        if (self.getStatus() != VolumeStatus.Deleted) {
            completion.fail(operr("the volume[uuid:%s, name:%s] is not deleted yet, can't expunge it",
                            self.getUuid(), self.getName()));
            return;
        }

        final VolumeInventory inv = getSelfInventory();
        String accountUuid = self.getAccountUuid();
        List<VolumeBeforeExpungeExtensionPoint> exts = pluginRgty.getExtensionList(VolumeBeforeExpungeExtensionPoint.class);
        exts.forEach(ext -> ext.volumePreExpunge(inv));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("expunge-volume");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "call-before-expunge-volume-extensions";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(exts).each((ext, c) -> ext.volumeBeforeExpunge(inv, new Completion(c) {
                            @Override
                            public void success() {
                                c.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.debug(String.format("failed to execute extension, because %s", errorCode.getDetails()));
                                c.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                if (self.getPrimaryStorageUuid() != null) {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-volume-%s-on-primary-storage", inv.getUuid());

                        @Override
                        public boolean skip(Map data) {
                            return exts.stream().anyMatch(ext -> ext.skipExpungeVolume(inv));
                        }

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                            dmsg.setVolume(getSelfInventory());
                            dmsg.setUuid(self.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply r) {
                                    if (!r.isSuccess() && dbf.isExist(dmsg.getPrimaryStorageUuid(), PrimaryStorageVO.class)) {
                                        trigger.fail(r.getError());
                                        return;
                                    }

                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("return-volume-%s-size-to-primary-storage", inv.getUuid());

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
                            msg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            msg.setDiskSize(self.getSize());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(msg);

                            /**
                             * After volume is deleted on primary storage and volume capacity returned,
                             * set primaryStorageUuid to null to confirm IncreasePrimaryStorageCapacityMsg
                             * only be send once.
                             */
                            self.setPrimaryStorageUuid(null);
                            self = dbf.updateAndRefresh(self);

                            trigger.next();
                        }
                    });
                }
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeAfterExpungeExtensionPoint.class), arg -> arg.volumeAfterExpunge(inv));

                callVolumeJustBeforeDeleteFromDbExtensionPoint();
                VolumeInventory volumeInventory = getSelfInventory();
                dbf.remove(self);
                cleanupVolumeEO(self.getUuid());
                completion.success();
                new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(VolumeStatus.Deleted, volumeInventory, accountUuid);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void cleanupVolumeEO(String volumeUuid) {
        boolean exists = Q.New(VolumeSnapshotEO.class)
                .eq(VolumeSnapshotEO_.volumeUuid, volumeUuid)
                .isExists();
        if (exists) {
            return;
        }

        dbf.eoCleanup(VolumeVO.class, self.getUuid());
    }

    private void handle(final ExpungeVolumeMsg msg) {
        final ExpungeVolumeReply reply = new ExpungeVolumeReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                expunge(new Completion(msg, chain) {
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
                return ExpungeVolumeMsg.class.getName();
            }
        });
    }

    private void handle(final CreateImageCacheFromVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doCreateImageCacheFromVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-template-from-data-volume-%s", msg.getVolumeUuid());
            }
        });
    }

    private void handle(final CreateDataVolumeTemplateFromDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return msg.isQueuedInVolume() ? syncThreadId : String.format("create-date-volume-template-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                doCreateDataVolumeTemplateFromDataVolumeMsg(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-template-from-data-volume-%s", msg.getVolumeUuid());
            }
        });
    }

    private void doCreateImageCacheFromVolume(CreateImageCacheFromVolumeMsg msg, NoErrorCompletion completion) {
        CreateImageCacheFromVolumeReply outReply = new CreateImageCacheFromVolumeReply();

        final CreateImageCacheFromVolumeOnPrimaryStorageMsg cmsg = new CreateImageCacheFromVolumeOnPrimaryStorageMsg();
        cmsg.setImageInventory(msg.getImage());
        cmsg.setVolumeInventory(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    outReply.setError(r.getError());
                } else {
                    CreateImageCacheFromVolumeOnPrimaryStorageReply reply = r.castReply();
                    outReply.setLocateHostUuid(reply.getLocateHostUuid());
                }

                bus.reply(msg, outReply);
                completion.done();
            }
        });
    }

    private void doCreateDataVolumeTemplateFromDataVolumeMsg(CreateDataVolumeTemplateFromDataVolumeMsg msg, NoErrorCompletion noErrorCompletion) {
        CreateTemplateFromVolumeOnPrimaryStorageMsg cmsg = new CreateTemplateFromVolumeOnPrimaryStorageMsg();
        if (msg instanceof CreateDataVolumeTemplateFromDataVolumeSnapshotMsg) {
            cmsg = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg();
            ((CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) cmsg).setSnapshotUuid(
                    ((CreateDataVolumeTemplateFromDataVolumeSnapshotMsg) msg).getSnapshotUuid());
        }

        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setImageInventory(ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class)));
        cmsg.setVolumeInventory(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg, noErrorCompletion) {
            @Override
            public void run(MessageReply r) {
                CreateDataVolumeTemplateFromDataVolumeReply reply = new CreateDataVolumeTemplateFromDataVolumeReply();
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                } else {
                    CreateTemplateFromVolumeOnPrimaryStorageReply creply = r.castReply();
                    String backupStorageInstallPath = creply.getTemplateBackupStorageInstallPath();
                    reply.setFormat(creply.getFormat());
                    reply.setInstallPath(backupStorageInstallPath);
                    reply.setMd5sum(null);
                    reply.setBackupStorageUuid(msg.getBackupStorageUuid());
                    reply.setActualSize(creply.getActualSize());
                }

                bus.reply(msg, reply);
                noErrorCompletion.done();
            }
        });
    }

    private void handle(final DeleteVolumeMsg msg) {
        final DeleteVolumeReply reply = new DeleteVolumeReply();
        delete(true, VolumeDeletionPolicy.valueOf(msg.getDeletionPolicy()),
                msg.isDetachBeforeDeleting(), new Completion(msg) {
                    @Override
                    public void success() {
                        logger.debug(String.format("deleted data volume[uuid: %s]", msg.getUuid()));
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
    }

    private void deleteVolume(final VolumeDeletionMsg msg, final NoErrorCompletion completion) {
        final VolumeDeletionReply reply = new VolumeDeletionReply();
        for (VolumeDeletionExtensionPoint extp : pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class)) {
            extp.preDeleteVolume(getSelfInventory());
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class), new ForEachFunction<VolumeDeletionExtensionPoint>() {
            @Override
            public void run(VolumeDeletionExtensionPoint arg) {
                arg.beforeDeleteVolume(getSelfInventory());
            }
        });


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-%s", self.getUuid()));
        // for NotInstantiated Volume, no flow to execute
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            VolumeDeletionPolicy deletionPolicy;

            {

                if (msg.getDeletionPolicy() == null) {
                    deletionPolicy = deletionPolicyMgr.getDeletionPolicy(self.getUuid());
                } else {
                    deletionPolicy = VolumeDeletionPolicy.valueOf(msg.getDeletionPolicy());
                }
            }

            @Override
            public void setup() {
                if (self.getVmInstanceUuid() != null && self.getType() == VolumeType.Data && msg.isDetachBeforeDeleting() &&
                        self.getStatus() != VolumeStatus.NotInstantiated && dbf.isExist(self.getVmInstanceUuid(), VmInstanceVO.class)) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "detach-volume-from-vm";

                        public void run(final FlowTrigger trigger, Map data) {
                            DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
                            dmsg.setVolume(getSelfInventory());
                            String vmUuid;
                            if (dmsg.getVmInstanceUuid() == null) {
                                vmUuid = getSelfInventory().getVmInstanceUuid();
                            } else {
                                vmUuid = dmsg.getVmInstanceUuid();
                            }
                            dmsg.setVmInstanceUuid(vmUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    self = dbf.reload(self);
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                if (deletionPolicy == VolumeDeletionPolicy.Direct) {
                    flow(new NoRollbackFlow() {
                        final List<VolumeStatus> allowedStatuses = Arrays.asList(VolumeStatus.Ready, VolumeStatus.Migrating);
                        String __name__ = "delete-volume-from-primary-storage";

                        @Override
                        public boolean skip(Map data) {
                            return pluginRgty.getExtensionList(VolumeBeforeExpungeExtensionPoint.class).stream()
                                    .anyMatch(ext -> ext.skipExpungeVolume(self.toInventory()));
                        }

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (allowedStatuses.contains(self.getStatus()) && self.getPrimaryStorageUuid() != null) {
                                if (self.getStatus() == VolumeStatus.Migrating) {
                                    self.setInstallPath(PathUtil.normalizePathWithoutQuery(self.getInstallPath()));
                                }

                                DeleteVolumeOnPrimaryStorageMsg dmsg = new DeleteVolumeOnPrimaryStorageMsg();
                                dmsg.setVolume(getSelfInventory());
                                dmsg.setUuid(self.getPrimaryStorageUuid());
                                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                                logger.debug(String.format("Asking primary storage[uuid:%s] to remove data volume[uuid:%s]", self.getPrimaryStorageUuid(),
                                        self.getUuid()));
                                bus.send(dmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            logger.warn(String.format("failed to delete volume[uuid:%s, name:%s], %s",
                                                    self.getUuid(), self.getName(), reply.getError()));
                                        }

                                        trigger.next();
                                    }
                                });
                            } else {
                                trigger.next();
                            }
                        }
                    });
                }


                if (self.getPrimaryStorageUuid() != null && deletionPolicy == VolumeDeletionPolicy.Direct) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "return-primary-storage-capacity";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                            imsg.setDiskSize(self.getSize());
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                            bus.send(imsg);
                            trigger.next();
                        }
                    });
                }

                flow(new Flow() {
                    String __name__ = "after-delete-volume-extension";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ErrorCodeList errList = new ErrorCodeList();
                        new While<>(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class)).each((ext, c) -> {
                            ext.afterDeleteVolume(getSelfInventory(), new Completion(c) {
                                @Override
                                public void success() {
                                    c.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    errList.getCauses().add(errorCode);
                                    c.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errList.getCauses().size() > 0) {
                                    trigger.fail(errList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });


                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        VolumeStatus oldStatus = self.getStatus();
                        String accountUuid = self.getAccountUuid();

                        if (deletionPolicy == VolumeDeletionPolicy.Direct) {
                            callVolumeJustBeforeDeleteFromDbExtensionPoint();
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            VolumeInventory volumeInventory = getSelfInventory();
                            dbf.remove(self);
                            cleanupVolumeEO(self.getUuid());
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, volumeInventory, accountUuid);
                        } else if (deletionPolicy == VolumeDeletionPolicy.Delay) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                        } else if (deletionPolicy == VolumeDeletionPolicy.Never) {
                            self.setStatus(VolumeStatus.Deleted);
                            self = dbf.updateAndRefresh(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());
                        } else if (deletionPolicy == VolumeDeletionPolicy.DBOnly) {
                            callVolumeJustBeforeDeleteFromDbExtensionPoint();
                            VolumeInventory inventory = getSelfInventory();
                            inventory.setStatus(VolumeStatus.Deleted.toString());
                            dbf.remove(self);
                            new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, inventory, accountUuid);
                        } else {
                            throw new CloudRuntimeException(String.format("Invalid deletionPolicy:%s", deletionPolicy));
                        }

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(final ErrorCode errCode, Map data) {
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeDeletionExtensionPoint.class),
                                new ForEachFunction<VolumeDeletionExtensionPoint>() {
                                    @Override
                                    public void run(VolumeDeletionExtensionPoint arg) {
                                        arg.failedToDeleteVolume(getSelfInventory(), errCode);
                                    }
                                });

                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                Finally(new FlowFinallyHandler(msg) {
                    @Override
                    public void Finally() {
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(OverwriteVolumeMsg msg) {
        OverwriteVolumeReply reply = new OverwriteVolumeReply();
        VolumeInventory volume = msg.getOriginVolume(),
                transientVolume = msg.getTransientVolume();

        if (transientVolume.isAttached() && !transientVolume.getAttachedVmUuids().equals(volume.getAttachedVmUuids())) {
            throw new CloudRuntimeException(String.format("transient volume[uuid:%s] has attached a different vm " +
                            "with origin volume[uuid:%s].",transientVolume.getUuid(), volume.getUuid()));
        }

        String newVolumeProvisioning = VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(transientVolume.getUuid(),
                VolumeVO.class, VOLUME_PROVISIONING_STRATEGY_TOKEN);
        VolumeDeletionPolicy originVolumeDeletionPolicy = pluginRgty.getExtensionList(ChangeVolumeProcessingMethodExtensionPoint.class)
                .stream().map(ext -> ext.getTransientVolumeDeletionPolicy(volume))
                .filter(Objects::nonNull).findFirst().orElse(VolumeDeletionPolicy.Direct);

        FlowChain chain = new SimpleFlowChain();
        chain.setName("cover-volume-from-transient-volume");
        chain.then(new NoRollbackFlow() {
            String __name__ = "swap-volume-path";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        // Do a swap to keep volume uuid ...
                        sql(VolumeVO.class)
                                .eq(VolumeVO_.uuid, transientVolume.getUuid())
                                .set(VolumeVO_.installPath, volume.getInstallPath())
                                .set(VolumeVO_.size, volume.getSize())
                                .set(VolumeVO_.rootImageUuid, volume.getRootImageUuid())
                                .set(VolumeVO_.primaryStorageUuid, volume.getPrimaryStorageUuid())
                                .set(VolumeVO_.actualSize, volume.getActualSize())
                                .set(VolumeVO_.protocol, volume.getProtocol())
                                .update();

                        sql(VolumeVO.class)
                                .eq(VolumeVO_.uuid, volume.getUuid())
                                .set(VolumeVO_.installPath, transientVolume.getInstallPath())
                                .set(VolumeVO_.size, transientVolume.getSize())
                                .set(VolumeVO_.rootImageUuid, transientVolume.getRootImageUuid())
                                .set(VolumeVO_.primaryStorageUuid, transientVolume.getPrimaryStorageUuid())
                                .set(VolumeVO_.actualSize, transientVolume.getActualSize())
                                .set(VolumeVO_.protocol, transientVolume.getProtocol())
                                .update();

                        pluginRgty.getExtensionList(OverwriteVolumeExtensionPoint.class).forEach(it ->
                                it.innerOverwriteVolume(volume, transientVolume, originVolumeDeletionPolicy));
                        flush();
                    }
                }.execute();
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "run-extension";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ChangeVolumeInstallPathExtensionPoint> exts = pluginRgty.getExtensionList(ChangeVolumeInstallPathExtensionPoint.class);

                if (!exts.isEmpty()) {
                    runExtensions(exts.iterator(), volume.getUuid(), transientVolume, trigger);
                } else {
                    trigger.next();
                }
            }

            private void runExtensions(final Iterator<ChangeVolumeInstallPathExtensionPoint> it, String volumeUuid, final VolumeInventory transientVolume, final FlowTrigger chain) {
                if (!it.hasNext()) {
                    chain.next();
                    return;
                }

                ChangeVolumeInstallPathExtensionPoint extp = it.next();

                logger.debug(String.format("run ChangeVolumeInstallPathExtensionPoint[%s]", extp.getClass()));
                extp.afterChangeVmVolumeInstallPath(volumeUuid, transientVolume, new Completion(chain) {
                    @Override
                    public void success() {
                        runExtensions(it, volumeUuid, transientVolume, chain);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-transient-volume-" + transientVolume.getUuid();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                DeleteVolumeMsg dmsg = new DeleteVolumeMsg();
                dmsg.setDeletionPolicy(originVolumeDeletionPolicy.toString());
                dmsg.setUuid(transientVolume.getUuid());
                dmsg.setDetachBeforeDeleting(true);
                bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeConstant.SERVICE_ID, transientVolume.getUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                afterOverwriteVolume();
                createSystemTag();
                bus.reply(msg, reply);
            }

            @ExceptionSafe
            private void afterOverwriteVolume() {
                pluginRgty.getExtensionList(OverwriteVolumeExtensionPoint.class).forEach(it ->
                        it.afterOverwriteVolume(volume, transientVolume));
            }

            private void createSystemTag() {
                SystemTagCreator creator = VolumeSystemTags.OVERWRITED_VOLUME.newSystemTagCreator(volume.getUuid());
                creator.setTagByTokens(Collections.singletonMap(VolumeSystemTags.OVERWRITED_VOLUME_TOKEN, transientVolume.getUuid()));
                creator.inherent = false;
                creator.create();

                if (newVolumeProvisioning != null) {
                    SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volume.getUuid());
                    tagCreator.setTagByTokens(
                            map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, newVolumeProvisioning))
                    );
                    tagCreator.inherent = false;
                    tagCreator.recreate = true;
                    tagCreator.create();
                }
            }

        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void callVolumeJustBeforeDeleteFromDbExtensionPoint() {
        VolumeInventory inv = getSelfInventory();
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeJustBeforeDeleteFromDbExtensionPoint.class), p -> p.volumeJustBeforeDeleteFromDb(inv));
    }

    private void handle(final VolumeDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                self = dbf.reload(self);
                if (self == null || self.getStatus() == VolumeStatus.Deleted) {
                    VolumeDeletionPolicy deletionPolicy = null;
                    if (self != null && msg.getDeletionPolicy() == null) {
                        deletionPolicy = deletionPolicyMgr.getDeletionPolicy(self.getUuid());
                    } else if (msg.getDeletionPolicy() != null){
                        deletionPolicy = VolumeDeletionPolicy.valueOf(msg.getDeletionPolicy());
                    }
                    if (deletionPolicy == VolumeDeletionPolicy.DBOnly) {
                        callVolumeJustBeforeDeleteFromDbExtensionPoint();
                        VolumeInventory inventory = getSelfInventory();
                        String accountUuid = self.getAccountUuid();
                        dbf.remove(self);
                        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(self.getStatus(), inventory, accountUuid);
                    }

                    // the volume has been deleted
                    // we run into this case because the cascading framework
                    // will send duplicate messages when deleting a vm as the cascading
                    // framework has no knowledge about if the volume has been deleted
                    VolumeDeletionReply reply = new VolumeDeletionReply();
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                deleteVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-%s", self.getUuid());
            }
        });
    }

    private void handle(GetVolumeBackingInstallPathMsg msg) {
        GetVolumeBackingInstallPathReply reply = new GetVolumeBackingInstallPathReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("get volume backing install path");
        chain.then(new ShareFlow() {
            String currentRootPath;
            List<String> previousRootPath;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "get-snapshot-root-node";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVolumeSnapshotTreeRootNodeMsg smsg = new GetVolumeSnapshotTreeRootNodeMsg();
                        smsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeLocalServiceId(smsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                GetVolumeSnapshotTreeRootNodeReply gr = r.castReply();
                                currentRootPath = gr.getCurrentRootInstallPath() == null ? self.getInstallPath() :
                                        gr.getCurrentRootInstallPath();
                                previousRootPath = gr.getPreviousRootInstallPaths();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-root-node-from-ps";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GetVolumeBackingChainFromPrimaryStorageMsg pmsg = new GetVolumeBackingChainFromPrimaryStorageMsg();
                        pmsg.setVolumeUuid(self.getUuid());
                        pmsg.setVolumeFormat(self.getFormat());
                        pmsg.getRootInstallPaths().addAll(previousRootPath);
                        pmsg.getRootInstallPaths().add(currentRootPath);
                        pmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        bus.makeLocalServiceId(pmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(pmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                 if (!r.isSuccess()) {
                                     trigger.fail(r.getError());
                                     return;
                                 }

                                 GetVolumeBackingChainFromPrimaryStorageReply gr = r.castReply();
                                 reply.setCurrentBackingChain(gr.getBackingChainInstallPath(currentRootPath));
                                 reply.setCurrentBackingChainSize(gr.getBackingChainSize(currentRootPath));
                                 previousRootPath.forEach(path -> {
                                     reply.addPreviousBackingChain(gr.getBackingChainInstallPath(path));
                                     reply.addPreviousBackingChainSize(gr.getBackingChainSize(path));
                                 });
                                 trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(SetVmBootVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                setBootVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("set-vm-boot-volume-%s", self.getUuid());
            }
        });
    }

    private void setBootVolume(SetVmBootVolumeMsg msg, NoErrorCompletion completion) {
        SetVmBootVolumeReply reply = new SetVmBootVolumeReply();

        self = dbf.reload(self);
        if (self.getType() == VolumeType.Root && msg.getVmInstanceUuid().equals(self.getVmInstanceUuid())) {
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("set-boot-volume");
        chain.then(new ShareFlow() {
            final List<VolumeSnapshotInventory> newRootSnapshots = new ArrayList<>();
            final List<VolumeSnapshotInventory> oldRootSnapshots = new ArrayList<>();
            VolumeInventory newRootVol;
            VolumeInventory oldRootVol;
            final Map<VolumeInventory, String> installPathsToGc = new HashMap<>();

            final VolumeVO originVol = Q.New(VolumeVO.class).eq(VolumeVO_.type, VolumeType.Root)
                    .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .find();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-operation-allowed";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<VolumeInventory> vols = new ArrayList<>();
                        vols.add(getSelfInventory());
                        vols.add(originVol.toInventory());
                        new While<>(vols).each((vol, compl) -> {
                            CheckChangeVolumeTypeOnPrimaryStorageMsg cmsg = new CheckChangeVolumeTypeOnPrimaryStorageMsg();
                            cmsg.setVolume(vol);
                            if (vol.getUuid().equals(self.getUuid())) {
                                cmsg.setTargetType(VolumeType.Root);
                            } else {
                                cmsg.setTargetType(VolumeType.Data);
                            }
                            bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                            bus.send(cmsg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        compl.addError(reply.getError());
                                        compl.allDone();
                                        return;
                                    }

                                    compl.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (!errorCodeList.getCauses().isEmpty()) {
                                    trigger.fail(errorCodeList.getCauses().get(0));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-new-root-volume-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ChangeVolumeTypeOnPrimaryStorageMsg cmsg = new ChangeVolumeTypeOnPrimaryStorageMsg();
                        cmsg.setSnapshots(VolumeSnapshotInventory.valueOf(Q.New(VolumeSnapshotVO.class)
                                .eq(VolumeSnapshotVO_.volumeUuid, self.getUuid())
                                .list()));
                        cmsg.setTargetType(VolumeType.Root);
                        cmsg.setVolume(getSelfInventory());
                        bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ChangeVolumeTypeOnPrimaryStorageReply cr = reply.castReply();
                                newRootSnapshots.addAll(cr.getSnapshots());
                                newRootVol = cr.getVolume();
                                if (cr.getInstallPathToGc() != null) {
                                    installPathsToGc.put(newRootVol, cr.getInstallPathToGc());
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-origin-root-volume-type";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ChangeVolumeTypeOnPrimaryStorageMsg cmsg = new ChangeVolumeTypeOnPrimaryStorageMsg();
                        cmsg.setTargetType(VolumeType.Data);
                        cmsg.setVolume(VolumeInventory.valueOf(originVol));
                        cmsg.setSnapshots(VolumeSnapshotInventory.valueOf(Q.New(VolumeSnapshotVO.class)
                                .eq(VolumeSnapshotVO_.volumeUuid, originVol.getUuid())
                                .list()));
                        bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ChangeVolumeTypeOnPrimaryStorageReply cr = reply.castReply();
                                oldRootSnapshots.addAll(cr.getSnapshots());
                                oldRootVol = cr.getVolume();
                                if (cr.getInstallPathToGc() != null) {
                                    installPathsToGc.put(oldRootVol, cr.getInstallPathToGc());
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "set-boot-volume-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                VmInstanceVO vm = findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                                Map<String, String> oldRootVolOldAndNewInstallPaths = new HashMap<>();
                                Map<String, String> newRootVolOldAndNewInstallPaths = new HashMap<>();

                                VolumeVO oldRootVolumeVO = vm.getRootVolume();
                                VolumeVO newRootVolumeVO = vm.getAllVolumes().stream().filter(it -> it.getUuid().equals(msg.getVolumeUuid()))
                                        .findFirst().orElseThrow(() -> new OperationFailureException(
                                                operr("volume[uuid%s] should be attached.")
                                        ));

                                oldRootVolOldAndNewInstallPaths.put(oldRootVolumeVO.getInstallPath(), oldRootVol.getInstallPath());
                                oldRootVolumeVO.setType(VolumeType.Data);
                                oldRootVolumeVO.setInstallPath(oldRootVol.getInstallPath());
                                oldRootVolumeVO.setDeviceId(newRootVolumeVO.getDeviceId());
                                merge(oldRootVolumeVO);

                                newRootVolOldAndNewInstallPaths.put(newRootVolumeVO.getInstallPath(), newRootVol.getInstallPath());
                                newRootVolumeVO.setType(VolumeType.Root);
                                newRootVolumeVO.setInstallPath(newRootVol.getInstallPath());
                                newRootVolumeVO.setDeviceId(0);
                                merge(newRootVolumeVO);

                                for (VolumeSnapshotInventory newRootSnapshot : newRootSnapshots) {
                                    VolumeSnapshotVO snapshot = findByUuid(newRootSnapshot.getUuid(), VolumeSnapshotVO.class);
                                    newRootVolOldAndNewInstallPaths.put(newRootSnapshot.getPrimaryStorageInstallPath(), newRootSnapshot.getPrimaryStorageInstallPath());
                                    snapshot.setVolumeType(VolumeType.Root.toString());
                                    snapshot.setPrimaryStorageInstallPath(newRootSnapshot.getPrimaryStorageInstallPath());
                                    merge(snapshot);
                                }

                                for (VolumeSnapshotInventory oldRootSnapshot : oldRootSnapshots) {
                                    VolumeSnapshotVO snapshot = findByUuid(oldRootSnapshot.getUuid(), VolumeSnapshotVO.class);
                                    oldRootVolOldAndNewInstallPaths.put(oldRootSnapshot.getPrimaryStorageInstallPath(), oldRootSnapshot.getPrimaryStorageInstallPath());
                                    snapshot.setVolumeType(VolumeType.Data.toString());
                                    snapshot.setPrimaryStorageInstallPath(oldRootSnapshot.getPrimaryStorageInstallPath());
                                    merge(snapshot);
                                }

                                // TODO use extension
                                oldRootVolOldAndNewInstallPaths.entrySet().removeIf(entry -> entry.getKey().equals(entry.getValue()));
                                newRootVolOldAndNewInstallPaths.entrySet().removeIf(entry -> entry.getKey().equals(entry.getValue()));
                                VolumeSnapshotReferenceUtils.handleVolumeInstallUrlChange(oldRootVolumeVO.getUuid(),
                                        oldRootVolOldAndNewInstallPaths);
                                VolumeSnapshotReferenceUtils.handleVolumeInstallUrlChange(newRootVolumeVO.getUuid(),
                                        newRootVolOldAndNewInstallPaths);

                                vm.setRootVolumeUuid(newRootVolumeVO.getUuid());
                                merge(vm);
                            }
                        }.execute();
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unlink-volumes-old-install-path";

                    @Override
                    public boolean skip(Map data) {
                        return installPathsToGc.isEmpty();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(installPathsToGc.entrySet()).each((entry, compl) -> {
                            VolumeInventory vol = entry.getKey();
                            UnlinkBitsOnPrimaryStorageMsg umsg = new UnlinkBitsOnPrimaryStorageMsg();
                            umsg.setInstallPath(entry.getValue());
                            umsg.setResourceUuid(vol.getUuid());
                            umsg.setResourceType(VolumeVO.class.getSimpleName());
                            umsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            bus.makeTargetServiceIdByResourceUuid(umsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                            bus.send(umsg, new CloudBusCallBack(compl) {
                                @Override
                                public void run(MessageReply reply) {
                                    compl.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(ChangeVolumeTypeMsg msg) {
        if (self.isAttached()) {
            ChangeVolumeTypeReply reply = new ChangeVolumeTypeReply();
            reply.setError(operr("only support detached volume, use SetVmBootVolumeMsg instead."));
            bus.reply(msg, reply);
            return;
        }


        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                changeVolumeType(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("change-volume-type-%s", self.getUuid());
            }
        });
    }

    private void changeVolumeType(ChangeVolumeTypeMsg msg, NoErrorCompletion completion) {
        if (self.getType() == msg.getType()) {
            bus.reply(msg, new ChangeVolumeTypeReply());
            completion.done();
            return;
        }

        ChangeVolumeTypeReply reply = new ChangeVolumeTypeReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("change-volume-type");
        chain.then(new ShareFlow() {
            final List<VolumeSnapshotInventory> changedSnapshots = new ArrayList<>();
            VolumeInventory changedVolume;
            String installPathToGc;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "change-volume-type-on-ps";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ChangeVolumeTypeOnPrimaryStorageMsg cmsg = new ChangeVolumeTypeOnPrimaryStorageMsg();
                        cmsg.setSnapshots(VolumeSnapshotInventory.valueOf(Q.New(VolumeSnapshotVO.class)
                                .eq(VolumeSnapshotVO_.volumeUuid, self.getUuid())
                                .list()));
                        cmsg.setTargetType(msg.getType());
                        cmsg.setVolume(getSelfInventory());
                        bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ChangeVolumeTypeOnPrimaryStorageReply cr = reply.castReply();
                                changedSnapshots.addAll(cr.getSnapshots());
                                changedVolume = cr.getVolume();
                                installPathToGc = cr.getInstallPathToGc();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-volume-type-in-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                Map<String, String> oldAndNewInstallPaths = new HashMap<>();
                                oldAndNewInstallPaths.put(self.getInstallPath(), changedVolume.getInstallPath());
                                self.setType(msg.getType());
                                self.setInstallPath(changedVolume.getInstallPath());
                                merge(self);

                                for (VolumeSnapshotInventory changedSnapshot : changedSnapshots) {
                                    VolumeSnapshotVO snapshot = findByUuid(changedSnapshot.getUuid(), VolumeSnapshotVO.class);
                                    oldAndNewInstallPaths.put(snapshot.getPrimaryStorageInstallPath(), changedSnapshot.getPrimaryStorageInstallPath());
                                    snapshot.setVolumeType(msg.getType().toString());
                                    snapshot.setPrimaryStorageInstallPath(changedSnapshot.getPrimaryStorageInstallPath());
                                    merge(snapshot);
                                }

                                // TODO use extension
                                oldAndNewInstallPaths.entrySet().removeIf(entry -> entry.getKey().equals(entry.getValue()));
                                VolumeSnapshotReferenceUtils.handleVolumeInstallUrlChange(self.getUuid(), oldAndNewInstallPaths);
                            }
                        }.execute();
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unlink-volume-old-install-path";

                    @Override
                    public boolean skip(Map data) {
                        return installPathToGc == null;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        UnlinkBitsOnPrimaryStorageMsg umsg = new UnlinkBitsOnPrimaryStorageMsg();
                        umsg.setInstallPath(installPathToGc);
                        umsg.setResourceUuid(self.getUuid());
                        umsg.setResourceType(VolumeVO.class.getSimpleName());
                        umsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(umsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                        bus.send(umsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.next();
                            }
                        });
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        completion.done();
                    }
                });
            }
        }).start();
    }

    private void handle(CreateVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-volume-%s-snapshot-group", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                CreateVolumeSnapshotGroupReply reply = new CreateVolumeSnapshotGroupReply();
                ErrorCode err = validateOperationByState(msg, self.getStatus(), SysErrors.OPERATION_ERROR);
                if (err != null) {
                    throw new OperationFailureException(err);
                }
                doCreateVolumeSnapshotGroup(msg, new ReturnValueCompletion<VolumeSnapshotGroupInventory>(chain) {
                    @Override
                    public void success(VolumeSnapshotGroupInventory inv) {
                        reply.setInventory(inv);
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
                return getSyncSignature();
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVolumeStateMsg) {
            handle((APIChangeVolumeStateMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            handle((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            handle((APICreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIDeleteDataVolumeMsg) {
            handle((APIDeleteDataVolumeMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromVmMsg) {
            handle((APIDetachDataVolumeFromVmMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            handle((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIGetDataVolumeAttachableVmMsg) {
            handle((APIGetDataVolumeAttachableVmMsg) msg);
        } else if (msg instanceof APIUpdateVolumeMsg) {
            handle((APIUpdateVolumeMsg) msg);
        } else if (msg instanceof APIRecoverDataVolumeMsg) {
            handle((APIRecoverDataVolumeMsg) msg);
        } else if (msg instanceof APIExpungeDataVolumeMsg) {
            handle((APIExpungeDataVolumeMsg) msg);
        } else if (msg instanceof APISyncVolumeSizeMsg) {
            handle((APISyncVolumeSizeMsg) msg);
        } else if (msg instanceof APIGetVolumeCapabilitiesMsg) {
            handle((APIGetVolumeCapabilitiesMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToHostMsg) {
            handle((APIAttachDataVolumeToHostMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromHostMsg) {
            handle((APIDetachDataVolumeFromHostMsg) msg);
        } else if (msg instanceof APIFlattenVolumeMsg) {
            handle((APIFlattenVolumeMsg) msg);
        } else if (msg instanceof APIUndoSnapshotCreationMsg) {
            handle((APIUndoSnapshotCreationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetVolumeCapabilitiesMsg msg) {
        APIGetVolumeCapabilitiesReply reply = new APIGetVolumeCapabilitiesReply();
        Map<String, Object> ret = new HashMap<String, Object>();
        if (VolumeStatus.Ready == self.getStatus()) {
            getPrimaryStorageCapacities(ret);
        }
        reply.setCapabilities(ret);
        bus.reply(msg, reply);
    }

    private void getPrimaryStorageCapacities(Map<String, Object> ret) {
        String type = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, self.getPrimaryStorageUuid())
                .findValue();

        PrimaryStorageType psType = PrimaryStorageType.valueOf(type);
        ret.put(Capability.MigrationInCurrentPrimaryStorage.toString(), psType.isSupportVolumeMigrationInCurrentPrimaryStorage());
        ret.put(Capability.MigrationToOtherPrimaryStorage.toString(), psType.isSupportVolumeMigrationToOtherPrimaryStorage());
    }

    private void syncVolumeVolumeSize(final ReturnValueCompletion<VolumeSize> completion) {
        refreshVO();
        SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();
        smsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
        smsg.setVolumeUuid(self.getUuid());
        smsg.setInstallPath(self.getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(smsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                refreshVO();
                SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                self.setSize(r.getSize());

                if (!r.isWithInternalSnapshot()) {
                    // the actual size = volume actual size + all snapshot size
                    long snapshotSize = calculateSnapshotSize();
                    self.setActualSize(r.getActualSize() + snapshotSize);
                } else {
                    self.setActualSize(r.getActualSize());
                }

                self = dbf.updateAndRefresh(self);

                VolumeSize size = new VolumeSize();
                size.actualSize = self.getActualSize();
                size.size = self.getSize();
                completion.success(size);
            }
        });
    }

    @Transactional(readOnly = true)
    private long calculateSnapshotSize() {
        String sql = "select sum(sp.size) from VolumeSnapshotVO sp  where sp.volumeUuid = :uuid";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("uuid", self.getUuid());
        Long size = q.getSingleResult();
        return size == null ? 0 : size;
    }

    private void handle(APISyncVolumeSizeMsg msg) {
        final APISyncVolumeSizeEvent evt = new APISyncVolumeSizeEvent(msg.getId());
        if (self.getStatus() != VolumeStatus.Ready) {
            evt.setInventory(getSelfInventory());
            bus.publish(evt);
            return;
        }

        syncVolumeVolumeSize(new ReturnValueCompletion<VolumeSize>(msg) {
            @Override
            public void success(VolumeSize ret) {
                evt.setInventory(getSelfInventory());
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIExpungeDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIExpungeDataVolumeEvent evt = new APIExpungeDataVolumeEvent(msg.getId());
                refreshVO();
                expunge(new Completion(msg, chain) {
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
                return msg.getClass().getName();
            }
        });
    }

    protected void recoverVolume(Completion completion) {
        final VolumeInventory vol = getSelfInventory();
        List<RecoverDataVolumeExtensionPoint> exts = pluginRgty.getExtensionList(RecoverDataVolumeExtensionPoint.class);

        CollectionUtils.safeForEach(exts, new ForEachFunction<RecoverDataVolumeExtensionPoint>() {
            @Override
            public void run(RecoverDataVolumeExtensionPoint ext) {
                ext.preRecoverDataVolume(vol);
            }
        });


        CollectionUtils.safeForEach(exts, new ForEachFunction<RecoverDataVolumeExtensionPoint>() {
            @Override
            public void run(RecoverDataVolumeExtensionPoint ext) {
                ext.beforeRecoverDataVolume(vol);
            }
        });

        VolumeStatus oldStatus = self.getStatus();

        if (self.getInstallPath() != null) {
            self.setStatus(VolumeStatus.Ready);
        } else {
            self.setStatus(VolumeStatus.NotInstantiated);
        }
        self = dbf.updateAndRefresh(self);

        new FireVolumeCanonicalEvent().fireVolumeStatusChangedEvent(oldStatus, getSelfInventory());

        CollectionUtils.safeForEach(exts, new ForEachFunction<RecoverDataVolumeExtensionPoint>() {
            @Override
            public void run(RecoverDataVolumeExtensionPoint ext) {
                ext.afterRecoverDataVolume(vol);
            }
        });
        completion.success();
    }

    private void handle(APIRecoverDataVolumeMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                final APIRecoverDataVolumeEvent evt = new APIRecoverDataVolumeEvent(msg.getId());
                recoverVolume(new Completion(msg, chain) {
                    @Override
                    public void success() {
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setInventory(getSelfInventory());
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getClass().getName();
            }
        });

    }

    private void handle(APIUpdateVolumeMsg msg) {
        boolean update = false;
        if (msg.getName() != null) {
            self.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            self = dbf.updateAndRefresh(self);
        }

        APIUpdateVolumeEvent evt = new APIUpdateVolumeEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    @Transactional
    private List<VmInstanceVO> getCandidateVmForAttaching(String accountUuid) {
        List<String> vmUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, VmInstanceVO.class);
        if (vmUuids != null && vmUuids.isEmpty()) {
            return new ArrayList<>();
        }

        SQL sql = null;
        if (self.getStatus() == VolumeStatus.Ready) {
            List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol" +
                    " where vol.uuid = :volUuid" +
                    " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                    (vmUuids == null ? "" : " and vm.uuid in (:vmUuids)") +
                    " and vm.clusterUuid = ref.clusterUuid" +
                    " and vm.type in (:vmTypes)" +
                    " and vm.state in (:vmStates)" +
                    " and vm.hypervisorType in (:hvTypes)" +
                    " group by vm.uuid")
                    .param("volUuid", self.getUuid())
                    .param("vmTypes", Arrays.asList(VmInstanceConstant.USER_VM_TYPE, "baremetal2"))
                    .param("hvTypes", hvTypes);
        } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, PrimaryStorageEO ps, PrimaryStorageCapacityVO capacity" +
                    " where "+ (vmUuids == null ? "" : " vm.uuid in (:vmUuids) and") +
                    " vm.state in (:vmStates)" +
                    " and vm.type in (:vmTypes)" +
                    " and vm.clusterUuid = ref.clusterUuid" +
                    " and capacity.uuid = ps.uuid" +
                    " and capacity.availableCapacity > :volumeSize" +
                    " and ref.primaryStorageUuid = ps.uuid" +
                    " and ps.state in (:psState)" +
                    " group by vm.uuid")
                    .param("volumeSize", self.getSize())
                    .param("vmTypes", Arrays.asList(VmInstanceConstant.USER_VM_TYPE, "baremetal2"))
                    .param("psState", PrimaryStorageState.Enabled);
        } else {
            DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
        }

        if (vmUuids != null) {
            sql.param("vmUuids", vmUuids);
        }
        List<VmInstanceVO> ret = sql.param("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped)).list();

        ret.addAll(getVmInstancesWithoutClusterInfo(vmUuids));
        //the vm doesn't suport to online attach volume when vm platform type is other
        ret.removeIf(it -> it.getPlatform().equals(ImagePlatform.Other.toString()) && it.getState() != VmInstanceState.Stopped);
        if (ret.isEmpty()) {
            return ret;
        }

        VolumeInventory vol = getSelfInventory();
        for (VolumeGetAttachableVmExtensionPoint ext : pluginRgty.getExtensionList(VolumeGetAttachableVmExtensionPoint.class)) {
            ret = ext.returnAttachableVms(vol, ret);
        }
        return ret;
    }

    private List<VmInstanceVO> getVmInstancesWithoutClusterInfo(List<String> accessibleVmUuids) {
        SQL sql = null;
        if (self.getStatus() == VolumeStatus.Ready) {
            String primaryStorageUuid = self.getPrimaryStorageUuid();

            List<String> hvTypes = VolumeFormat.valueOf(self.getFormat()).getHypervisorTypesSupportingThisVolumeFormatInString();
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, VolumeVO vol" +
                    " where vol.uuid = vm.rootVolumeUuid" +
                    " and ref.primaryStorageUuid = :primaryStorageUuid" +
                    " and vol.primaryStorageUuid = :primaryStorageUuid" +
                    (accessibleVmUuids == null ? "" : " and vm.uuid in (:vmUuids)") +
                    " and vm.clusterUuid is NULL" +
                    " and vm.type = :vmType" +
                    " and vm.state in (:vmStates)" +
                    " and vm.hypervisorType in (:hvTypes)" +
                    " group by vm.uuid")
                    .param("primaryStorageUuid", primaryStorageUuid)
                    .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                    .param("hvTypes", hvTypes);
        } else if (self.getStatus() == VolumeStatus.NotInstantiated) {
            sql = SQL.New("select vm" +
                    " from VmInstanceVO vm, PrimaryStorageClusterRefVO ref, PrimaryStorageEO ps, PrimaryStorageCapacityVO capacity" +
                    " where "+ (accessibleVmUuids == null ? "" : " vm.uuid in (:vmUuids) and") +
                    " vm.state in (:vmStates)" +
                    " and vm.type = :vmType" +
                    " and vm.clusterUuid is NULL" +
                    " and capacity.uuid = ps.uuid" +
                    " and capacity.availableCapacity > :volumeSize" +
                    " and ref.primaryStorageUuid = ps.uuid" +
                    " and ps.state in (:psState)" +
                    " group by vm.uuid")
                    .param("volumeSize", self.getSize())
                    .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                    .param("psState", PrimaryStorageState.Enabled);
        } else {
            DebugUtils.Assert(false, String.format("should not reach here, volume[uuid:%s]", self.getUuid()));
        }

        if (accessibleVmUuids != null) {
            sql.param("vmUuids", accessibleVmUuids);
        }

        return sql.param("vmStates", Arrays.asList(VmInstanceState.Running, VmInstanceState.Stopped)).list();
    }

    private boolean volumeIsAttached(final String volumeUuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .notNull(VolumeVO_.vmInstanceUuid)
                .isExists();
    }

    private void handle(APIGetDataVolumeAttachableVmMsg msg) {
        APIGetDataVolumeAttachableVmReply reply = new APIGetDataVolumeAttachableVmReply();
        if (volumeIsAttached(msg.getVolumeUuid())) {
            reply.setInventories(VmInstanceInventory.valueOf(new ArrayList<>()));
        } else {
            reply.setInventories(VmInstanceInventory.valueOf(getCandidateVmForAttaching(msg.getSession().getAccountUuid())));
        }
        bus.reply(msg, reply);
    }

    private void handle(final APIAttachDataVolumeToVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                AttachDataVolumeToVmMsg amsg = new AttachDataVolumeToVmMsg();
                amsg.setVolume(getSelfInventory());
                amsg.setVmInstanceUuid(msg.getVmInstanceUuid());
                bus.makeTargetServiceIdByResourceUuid(amsg, VmInstanceConstant.SERVICE_ID, amsg.getVmInstanceUuid());
                bus.send(amsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        final APIAttachDataVolumeToVmEvent evt = new APIAttachDataVolumeToVmEvent(msg.getId());
                        self = dbf.reload(self);
                        if (reply.isSuccess()) {
                            evt.setInventory(getSelfInventory());
                        } else {
                            if (self.getVmInstanceUuid() != null) {
                                self.setVmInstanceUuid(null);
                                dbf.update(self);
                            }

                            evt.setError(reply.getError());
                        }

                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getClass().getName();
            }
        });

    }

    private void handle(final APIDetachDataVolumeFromVmMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshVO();
                DetachDataVolumeFromVmMsg dmsg = new DetachDataVolumeFromVmMsg();
                dmsg.setVolume(getSelfInventory());
                String vmUuid;
                if (msg.getVmUuid() != null) {
                    vmUuid = msg.getVmUuid();
                } else {
                    vmUuid = getSelfInventory().getVmInstanceUuid();
                }
                dmsg.setVmInstanceUuid(vmUuid);

                bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                bus.send(dmsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        APIDetachDataVolumeFromVmEvent evt = new APIDetachDataVolumeFromVmEvent(msg.getId());
                        if (reply.isSuccess()) {
                            refreshVO();
                            evt.setInventory(getSelfInventory());
                        } else {
                            evt.setError(reply.getError());
                        }

                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return msg.getClass().getName();
            }
        });
    }

    protected VolumeInventory getSelfInventory() {
        return VolumeInventory.valueOf(self);
    }

    private void delete(boolean forceDelete, final Completion completion) {
        delete(forceDelete, true, completion);
    }

    // don't put this in queue, it will eventually send the VolumeDeletionMsg that will be in queue
    private void delete(boolean forceDelete, boolean detachBeforeDeleting, final Completion completion) {
        delete(forceDelete, null, detachBeforeDeleting, completion);
    }

    private void delete(boolean forceDelete, VolumeDeletionPolicy deletionPolicy, boolean detachBeforeDeleting, final Completion completion) {
        final String issuer = VolumeVO.class.getSimpleName();
        VolumeDeletionStruct struct = new VolumeDeletionStruct();
        struct.setInventory(getSelfInventory());
        struct.setDetachBeforeDeleting(detachBeforeDeleting);
        struct.setDeletionPolicy(deletionPolicy != null ? deletionPolicy.toString() : deletionPolicyMgr.getDeletionPolicy(self.getUuid()).toString());
        final List<VolumeDeletionStruct> ctx = list(struct);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("delete-data-volume");
        if (!forceDelete) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIDeleteDataVolumeMsg msg) {
        final APIDeleteDataVolumeEvent evt = new APIDeleteDataVolumeEvent(msg.getId());
        delete(msg.getDeletionMode() == DeletionMode.Enforcing, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final VolumeCreateSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return msg.isQueuedInVolume() ? syncThreadId : String.format("volume-%s-create-snapshot", msg.getVolumeUuid());
            }

            @Override

            public void run(final SyncTaskChain chain) {
                ErrorCode err = validateOperationByState(msg, self.getStatus(), SysErrors.OPERATION_ERROR);
                if (err != null) {
                    throw new OperationFailureException(err);
                }

                CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                cmsg.setName(msg.getName());
                cmsg.setDescription(msg.getDescription());
                cmsg.setResourceUuid(msg.getResourceUuid());
                cmsg.setAccountUuid(msg.getAccountUuid());
                cmsg.setVolumeUuid(msg.getVolumeUuid());
                cmsg.setRequiredSnapshotMode(msg.getRequiredSnapshotMode());
                bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                bus.send(cmsg, new CloudBusCallBack(msg, chain) {
                    @Override
                    public void run(MessageReply reply) {
                        VolumeCreateSnapshotReply r = new VolumeCreateSnapshotReply();
                        if (reply.isSuccess()) {
                            CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                            syncVolSize();
                            r.setInventory(creply.getInventory());
                        } else {
                            r.setError(reply.getError());
                        }

                        bus.reply(msg, r);
                        chain.next();
                    }

                    private void syncVolSize() {
                        SyncVolumeSizeMsg syncVolumeSizeMsg = new SyncVolumeSizeMsg();
                        syncVolumeSizeMsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(syncVolumeSizeMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                        bus.send(syncVolumeSizeMsg);
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-snapshot-for-volume-%s", self.getUuid());
            }
        });
    }

    private void handle(final APICreateVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncThreadId;
            }

            @Override
            public void run(final SyncTaskChain taskChain) {
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("");

                chain.then(new NoRollbackFlow() {
                    String __name__ = String.format("create-snapshot-for-volume-%s", msg.getVolumeUuid());
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName(msg.getName());
                        cmsg.setDescription(msg.getDescription());
                        cmsg.setResourceUuid(msg.getResourceUuid());
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
                        cmsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    CreateVolumeSnapshotReply creply = (CreateVolumeSnapshotReply) reply;
                                    tagMgr.createTagsFromAPICreateMessage(msg, creply.getInventory().getUuid(), VolumeSnapshotVO.class.getSimpleName());
                                    data.put("uuid", creply.getInventory().getUuid());
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = String.format("sync-volume[uuid: %s]-size-after-create-snapshot", msg.getVolumeUuid());
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SyncVolumeSizeMsg syncVolumeSizeMsg = new SyncVolumeSizeMsg();
                        syncVolumeSizeMsg.setVolumeUuid(msg.getVolumeUuid());
                        bus.makeTargetServiceIdByResourceUuid(syncVolumeSizeMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                        bus.send(syncVolumeSizeMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.next();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(taskChain) {
                    @Override
                    public void handle(Map data) {
                        APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(msg.getId());
                        String snapshotUuid = (String)data.get("uuid");

                        evt.setInventory(VolumeSnapshotInventory.valueOf(dbf.findByUuid(snapshotUuid, VolumeSnapshotVO.class)));
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).error(new FlowErrorHandler(taskChain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        APICreateVolumeSnapshotEvent evt = new APICreateVolumeSnapshotEvent(msg.getId());
                        evt.setError(errCode);
                        bus.publish(evt);
                        taskChain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return String.format("create-snapshot-for-volume-%s", self.getUuid());
            }
        });
    }

    private void handle(APICreateVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-volume-%s-snapshot-group", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICreateVolumeSnapshotGroupEvent evt = new APICreateVolumeSnapshotGroupEvent(msg.getId());
                doCreateVolumeSnapshotGroup(msg, new ReturnValueCompletion<VolumeSnapshotGroupInventory>(chain) {
                    @Override
                    public void success(VolumeSnapshotGroupInventory inv) {
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

    private void doCreateVolumeSnapshotGroup(CreateVolumeSnapshotGroupMessage msg, ReturnValueCompletion<VolumeSnapshotGroupInventory> completion) {
        final String SNAPSHOT_GROUP_INV = "SNAPSHOT_GROUP_INV";

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("create-volume-%s-snapshot-group", msg.getRootVolumeUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-operation-on-primary-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, List<String>> psVolumeRef = msg.getVmInstance().getAllDiskVolumes().stream()
                        .collect(Collectors.groupingBy(VolumeInventory::getPrimaryStorageUuid,
                                Collectors.mapping(VolumeInventory::getUuid, Collectors.toList())));

                new While<>(psVolumeRef.entrySet()).each((e, c) -> {
                    CheckVolumeSnapshotOperationOnPrimaryStorageMsg cmsg = new CheckVolumeSnapshotOperationOnPrimaryStorageMsg();
                    cmsg.setPrimaryStorageUuid(e.getKey());
                    cmsg.setVolumeUuids(e.getValue());
                    cmsg.setVmInstanceUuid(msg.getVmInstance().getUuid());
                    cmsg.setOperation(msg.getBackendOperation());
                    bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
                    bus.send(cmsg, new CloudBusCallBack(c) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                c.addError(reply.getError());
                                c.allDone();
                                return;
                            }

                            c.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }

                    }
                });
            }
        });

        List<VolumeSnapshotCreationExtensionPoint> exts = pluginRgty.getExtensionList(VolumeSnapshotCreationExtensionPoint.class);
        List<Flow> flows = exts
                .stream()
                .map(ext -> ext.beforeCreateVolumeSnapshotFlow(msg))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        flows.forEach(chain::then);

        chain.then(new Flow() {
            String __name__ = "take-snapshots";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                msg.setVmInstance(VmInstanceInventory.valueOf(dbf.findByUuid(msg.getVmInstance().getUuid(), VmInstanceVO.class)));
                createSnapshotGroup(msg, new ReturnValueCompletion<VolumeSnapshotGroupInventory>(trigger) {
                    @Override
                    public void success(VolumeSnapshotGroupInventory returnValue) {
                        data.put(SNAPSHOT_GROUP_INV, returnValue);
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
                if (data.get(SNAPSHOT_GROUP_INV) == null) {
                    trigger.rollback();
                    return;
                }
                DeleteVolumeSnapshotGroupInnerMsg imsg = new DeleteVolumeSnapshotGroupInnerMsg();
                imsg.setUuid(((VolumeSnapshotGroupInventory) data.get(SNAPSHOT_GROUP_INV)).getUuid());
                imsg.setDeletionMode(DeletionMode.Permissive);
                imsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                imsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                bus.makeTargetServiceIdByResourceUuid(imsg, VolumeSnapshotConstant.SERVICE_ID, imsg.getUuid());
                VolumeSnapshotGroupOverlayMsg omsg = new VolumeSnapshotGroupOverlayMsg();
                omsg.setVmInstanceUuid(self.getVmInstanceUuid());
                omsg.setMessage((NeedReplyMessage) imsg);
                bus.makeTargetServiceIdByResourceUuid(omsg, VmInstanceConstant.SERVICE_ID, self.getVmInstanceUuid());
                bus.send(omsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.debug(String.format("failed to delete VolumeSnapshotGroup[uuid: %s]: %s", imsg.getUuid(), reply.getError()));
                        }

                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "sync-vm-devices-address-info";
            @Override
            public boolean skip(Map data) {
                return msg.getConsistentType() != ConsistentType.Application;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncVmDevicesAddressInfo(msg.getVmInstance(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "after-volume-snapshot-group-created";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(pluginRgty.getExtensionList(VolumeSnapshotCreationExtensionPoint.class)).each((ext, compl) -> {
                    ext.afterVolumeSnapshotGroupCreated(((VolumeSnapshotGroupInventory) data.get(SNAPSHOT_GROUP_INV)), msg.getConsistentType(), new Completion(compl) {
                        @Override
                        public void success() {
                            compl.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            compl.addError(errorCode);
                            compl.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(errorCodeList.getCauses().get(0));
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "sync-vm-status-for-memory-snapshot-group";

            public boolean skip(Map data) {
                return msg.getConsistentType() != ConsistentType.Application;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmCheckOwnStateMsg cmsg = new VmCheckOwnStateMsg();
                cmsg.setVmInstanceUuid(msg.getVmInstance().getUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstance().getUuid());
                bus.send(cmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success((VolumeSnapshotGroupInventory) data.get(SNAPSHOT_GROUP_INV));
            }
        }).start();
    }

    private void syncVmDevicesAddressInfo(VmInstanceInventory vm, Completion completion) {
        SyncVmDeviceInfoMsg msg = new SyncVmDeviceInfoMsg();
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setHostUuid(vm.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                completion.success();
            }
        });

    }

    private void createSnapshotGroup(CreateVolumeSnapshotGroupMessage msg, ReturnValueCompletion<VolumeSnapshotGroupInventory> completion) {
        VolumeSnapshotGroupOperationValidator.validate(msg.getVmInstance().getUuid(), VolumeSnapshotGroupOperationValidator.Operation.CREATE);
        CreateVolumesSnapshotMsg cmsg = new CreateVolumesSnapshotMsg();
        List<CreateVolumesSnapshotsJobStruct> volumesSnapshotsJobs = new ArrayList<>();
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        cmsg.setBackupHostFileIfNeeded(true);

        VmInstanceInventory vm = msg.getVmInstance();
        Map<String, VolumeInventory> vols = vm.getAllVolumes().stream()
                .filter(it -> it.isDisk() || msg.getConsistentType() == ConsistentType.Application
                        && it.getType().equals(VolumeType.Memory.toString()))
                .collect(Collectors.toMap(VolumeInventory::getUuid, it -> it));
        boolean withMemory = msg.getConsistentType() == ConsistentType.Application;

        for (VolumeInventory vol : vols.values()) {
            CreateVolumesSnapshotsJobStruct volumesSnapshotsJob = new CreateVolumesSnapshotsJobStruct();

            volumesSnapshotsJob.setVolumeUuid(vol.getUuid());
            volumesSnapshotsJob.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
            volumesSnapshotsJob.setResourceUuid(getUuid());
            volumesSnapshotsJob.setName(msg.getName() + "-" + vol.getName());
            volumesSnapshotsJob.setDescription(msg.getDescription());
            volumesSnapshotsJobs.add(volumesSnapshotsJob);
        }
        cmsg.setVolumeSnapshotJobs(volumesSnapshotsJobs);
        cmsg.setConsistentType(msg.getConsistentType());

        List<VolumeSnapshotInventory> inventories = new ArrayList<>();
        List<String> hostBackupFileUuidList = new ArrayList<>();
        AtomicReference<VolumeSnapshotGroupVO> groupRef = new AtomicReference<>(null);
        String resourceUuid = msg.getResourceUuid() == null ? getUuid() : msg.getResourceUuid();

        SimpleFlowChain.of("create-snapshot-group")
            .then("handle-create-snapshot-group-message", trigger -> {
                bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, msg.getRootVolumeUuid());
                bus.send(cmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        CreateVolumesSnapshotReply r = reply.castReply();
                        inventories.addAll(r.getInventories());
                        if (!CollectionUtils.isEmpty(r.getHostBackupFileUuidList())) {
                            hostBackupFileUuidList.addAll(r.getHostBackupFileUuidList());
                        }
                        trigger.next();
                    }
                });
            })
            .then(Flow.of("persist-snapshot-group")
                .handle(trigger -> {
                    List<VolumeSnapshotGroupRefVO> refs = new ArrayList<>();
                    VolumeSnapshotGroupVO group = new VolumeSnapshotGroupVO();
                    group.setUuid(resourceUuid);
                    group.setSnapshotCount(cmsg.getVolumeSnapshotJobs().size());
                    group.setName(msg.getName());
                    group.setDescription(msg.getDescription());
                    group.setVmInstanceUuid(vm.getUuid());
                    group.setAccountUuid(msg.getSession().getAccountUuid());
                    for (VolumeSnapshotInventory inv : inventories) {
                        VolumeSnapshotGroupRefVO ref = new VolumeSnapshotGroupRefVO();
                        ref.setVolumeUuid(inv.getVolumeUuid());
                        ref.setVolumeName(vols.get(inv.getVolumeUuid()).getName());
                        ref.setVolumeType(inv.getVolumeType());
                        ref.setVolumeSnapshotGroupUuid(group.getUuid());
                        ref.setVolumeSnapshotUuid(inv.getUuid());
                        ref.setVolumeSnapshotName(inv.getName());
                        ref.setVolumeSnapshotInstallPath(inv.getPrimaryStorageInstallPath());
                        ref.setDeviceId(vols.get(inv.getVolumeUuid()).getDeviceId());
                        ref.setVolumeLastAttachDate(vols.get(inv.getVolumeUuid()).getLastAttachDate());
                        refs.add(ref);
                    }

                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            databaseFacade.persist(group);
                            databaseFacade.persistCollection(refs);
                        }
                    }.execute();

                    groupRef.set(group);
                    trigger.next();
                })
                .rollback(trigger -> {
                    SQL.New(VolumeSnapshotGroupVO.class)
                            .eq(VolumeSnapshotGroupVO_.uuid, resourceUuid)
                            .delete();
                    // VolumeSnapshotGroupRefVO delete in cascade
                    trigger.rollback();
                })
                .build())
            .then(Flow.of("persist-vm-host-backup-file")
                .handle(trigger -> {
                    if (!CollectionUtils.isEmpty(hostBackupFileUuidList)) {
                        SQL.New(VmHostBackupFileVO.class)
                                .in(VmHostBackupFileVO_.uuid, hostBackupFileUuidList)
                                .set(VmHostBackupFileVO_.resourceUuid, resourceUuid)
                                .update();
                    }
                    trigger.next();
                })
                .rollback(trigger -> {
                    if (CollectionUtils.isEmpty(hostBackupFileUuidList)) {
                        trigger.rollback();
                        return;
                    }

                    new While<>(hostBackupFileUuidList).each((uuid, whileCompletion) -> {
                        VmHostBackupFileDeletionMsg deletionMsg = new VmHostBackupFileDeletionMsg();
                        deletionMsg.setUuid(uuid);
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
                    }).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            if (errorCodeList.hasError()) {
                                logger.warn("failed to delete some VmHostBackupFiles:\n" + String.join("\n",
                                        transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails)));
                            }
                            trigger.rollback();
                        }
                    });
                })
                .build())
            .then(Flow.of("backup-encrypted-resource-key-ref-for-snapshot-group")
                .skipIf(data -> CollectionUtils.isEmpty(hostBackupFileUuidList) || !withMemory)
                .handle(trigger -> {
                    String tpmUuid = Q.New(TpmVO.class)
                            .eq(TpmVO_.vmInstanceUuid, vm.getUuid())
                            .select(TpmVO_.uuid)
                            .findValue();
                    if (tpmUuid == null) {
                        trigger.next();
                        return;
                    }

                    tagMgr.createInherentSystemTag(resourceUuid,
                            VolumeSnapshotGroupSystemTags.WITH_TPM.getTagFormat(),
                            VolumeSnapshotGroupVO.class.getSimpleName());

                    BackupTpmEncryptionKeyMsg backupMsg = new BackupTpmEncryptionKeyMsg();
                    backupMsg.setSrcResourceUuid(tpmUuid);
                    backupMsg.setDstResourceUuid(resourceUuid);
                    bus.makeLocalServiceId(backupMsg, TpmConstants.SERVICE_ID);
                    bus.send(backupMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                trigger.fail(reply.getError());
                                return;
                            }
                            trigger.next();
                        }
                    });
                })
                .build())
            .propagateExceptionTo(completion)
            .done(() -> {
                logger.debug(String.format("created volume snapshot group[uuid:%s] for vm[uuid:%s]",
                        groupRef.get().getUuid(), vm.getUuid()));
                completion.success(VolumeSnapshotGroupInventory.valueOf(dbf.reload(groupRef.get())));
            })
            .error(completion::fail)
            .start();
    }

    private void handle(APIFlattenVolumeMsg msg) {
        FlattenVolumeMsg fmsg = new FlattenVolumeMsg();
        fmsg.setUuid(msg.getUuid());
        fmsg.setDryRun(msg.isDryRun());
        bus.makeTargetServiceIdByResourceUuid(fmsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
        bus.send(fmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIFlattenVolumeEvent evt = new APIFlattenVolumeEvent(msg.getId());
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    FlattenVolumeReply fr = reply.castReply();
                    evt.setInventory(fr.getInventory());
                }
                bus.publish(evt);;
            }
        });
    }

    private void handle(FlattenVolumeMsg msg) {
        if (msg.isDryRun()) {
            FlattenVolumeReply reply = new FlattenVolumeReply();
            estimateTemplateSize(new ReturnValueCompletion<EstimateVolumeTemplateSizeReply>(msg) {
                @Override
                public void success(EstimateVolumeTemplateSizeReply er) {
                    long templateSize = er.getActualSize();
                    if (!er.isWithInternalSnapshot()) {
                        templateSize += calculateSnapshotSize();
                    }

                    self.setActualSize(templateSize);
                    reply.setInventory(getSelfInventory());
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("flattern-volume-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                FlattenVolumeReply reply = new FlattenVolumeReply();
                flattenVolume(new Completion(chain) {
                    @Override
                    public void success() {
                        refreshVO();
                        reply.setInventory(getSelfInventory());
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(err(VolumeErrors.FLATTEN_ERROR, "failed to flatten volume[uuid:%s]", self.getUuid())
                                .withCause(errorCode));
                        bus.reply(msg, reply);
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

    private void handle(CancelFlattenVolumeMsg msg) {
        String hostUuid = !self.isAttached() ? null : Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, self.getVmInstanceUuid())
                .findValue();

        CancelFlattenVolumeReply reply = new CancelFlattenVolumeReply();
        Completion completion = new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };
        if (hostUuid == null) {
            cancelVolumeTaskOffline(msg.getCancellationApiId(), completion);
        } else {
            cancelVolumeTaskOnline(hostUuid, msg.getCancellationApiId(), completion);
        }
    }

    @VmAttachVolumeValidatorMethod
    static void vmAttachVolumeValidator(VmInstanceInventory vmInv, String volumeUuid) {
        String vmUuid = vmInv.getUuid();
        String hypervisorType = vmInv.getHypervisorType();

        PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
        MaxDataVolumeNumberExtensionPoint ext = pluginRgty.getExtensionFromMap(hypervisorType, MaxDataVolumeNumberExtensionPoint.class);
        int maxDataVolumeNum = ext == null ? VolumeConstant.DEFAULT_MAX_DATA_VOLUME_NUMBER : ext.getMaxDataVolumeNumber();

        long vmDataVolumeUsage = Q.New(VolumeVO.class)
                .eq(VolumeVO_.type, VolumeType.Data)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .count();
        if (vmDataVolumeUsage + 1 > maxDataVolumeNum) {
            throw new OperationFailureException(operr("hypervisor[%s] only allows max %s data volumes to be attached to a single vm; there have been %s data volumes attached to vm[uuid:%s]",
                    hypervisorType, maxDataVolumeNum, vmDataVolumeUsage, vmUuid)
                    .withOpaque("max.volume.count", maxDataVolumeNum));
        }
    }

    private void flattenVolume(Completion completion) {
        FlattenVolumeOnPrimaryStorageMsg mmsg = new FlattenVolumeOnPrimaryStorageMsg();
        mmsg.setVolume(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(mmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                pluginRgty.getExtensionList(FlattenVolumeExtensionPoint.class).forEach(it ->
                        it.afterFlattenVolume(getSelfInventory()));

                SyncVolumeSizeMsg smsg = new SyncVolumeSizeMsg();
                smsg.setVolumeUuid(self.getUuid());
                bus.makeLocalServiceId(smsg, VolumeConstant.SERVICE_ID);
                bus.send(smsg);
                completion.success();
            }
        });
    }

    private void cancelVolumeTaskOnline(String hostUuid, String apiId, Completion completion) {
        CancelHostTaskMsg cmsg = new CancelHostTaskMsg();
        cmsg.setCancellationApiId(apiId);
        bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success();
            }
        });
    }

    private void cancelVolumeTaskOffline(String apiId, Completion completion) {
        CancelJobOnPrimaryStorageMsg cmsg = new CancelJobOnPrimaryStorageMsg();
        cmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
        cmsg.setCancellationApiId(apiId);
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success();
            }
        });
    }

    private void estimateTemplateSize(ReturnValueCompletion<EstimateVolumeTemplateSizeReply> completion) {
        EstimateVolumeTemplateSizeMsg msg = new EstimateVolumeTemplateSizeMsg();
        msg.setIgnoreError(false);
        msg.setVolumeUuid(self.getUuid());
        bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                EstimateVolumeTemplateSizeReply sr = reply.castReply();
                completion.success(sr);
            }
        });
    }

    private void handle(APIChangeVolumeStateMsg msg) {
        VolumeStateEvent sevt = VolumeStateEvent.valueOf(msg.getStateEvent());
        if (sevt == VolumeStateEvent.enable) {
            self.setState(VolumeState.Enabled);
        } else {
            self.setState(VolumeState.Disabled);
        }
        self = dbf.updateAndRefresh(self);
        VolumeInventory inv = VolumeInventory.valueOf(self);
        APIChangeVolumeStateEvent evt = new APIChangeVolumeStateEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    class VolumeSize {
        long size;
        long actualSize;
    }

    private void handle(APIAttachDataVolumeToHostMsg msg) {
        APIAttachDataVolumeToHostEvent evt = new APIAttachDataVolumeToHostEvent(msg.getId());
        AttachDataVolumeToHostMsg mmsg = new AttachDataVolumeToHostMsg();
        mmsg.setHostUuid(msg.getHostUuid());
        mmsg.setVolumeUuid(msg.getVolumeUuid());
        mmsg.setMountPath(msg.getMountPath());
        bus.makeTargetServiceIdByResourceUuid(mmsg, HostConstant.SERVICE_ID, mmsg.getHostUuid());
        bus.send(mmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDetachDataVolumeFromHostMsg msg) {
        APIDetachDataVolumeFromHostEvent evt = new APIDetachDataVolumeFromHostEvent(msg.getId());
        VolumeHostRefVO ref = Q.New(VolumeHostRefVO.class).eq(VolumeHostRefVO_.volumeUuid, msg.getVolumeUuid()).find();
        String hostUuid = msg.getHostUuid() != null ? msg.getHostUuid() : ref.getHostUuid();

        DetachDataVolumeFromHostMsg dmsg = new DetachDataVolumeFromHostMsg();
        dmsg.setVolumeInstallPath(self.getInstallPath());
        dmsg.setMountPath(ref.getMountPath());
        dmsg.setDevice(ref.getDevice());
        dmsg.setHostUuid(hostUuid);
        dmsg.setVolumeUuid(msg.getVolumeUuid());
        bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, dmsg.getHostUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                    return;
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUndoSnapshotCreationMsg msg) {
        APIUndoSnapshotCreationEvent evt = new APIUndoSnapshotCreationEvent(msg.getId());

        VolumeSnapshotVO snapShot = dbf.findByUuid(msg.getSnapShotUuid(), VolumeSnapshotVO.class);
        DeleteVolumeSnapshotMsg dmsg = new DeleteVolumeSnapshotMsg();
        dmsg.setTreeUuid(snapShot.getTreeUuid());
        dmsg.setVolumeUuid(snapShot.getVolumeUuid());
        dmsg.setSnapshotUuid(snapShot.getUuid());
        dmsg.setDeletionMode(APIDeleteMessage.DeletionMode.Permissive);
        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());

        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapShot.getUuid());
        bus.send(dmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    evt.setError(r.getError());
                    bus.publish(evt);
                    return;
                }
                VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, snapShot.getVolumeUuid()).find();
                evt.setInventory(VolumeInventory.valueOf(volumeVO));
                bus.publish(evt);
            }
        });
    }
}
