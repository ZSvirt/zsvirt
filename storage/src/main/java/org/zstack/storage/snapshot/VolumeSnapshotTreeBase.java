package org.zstack.storage.snapshot;

import org.apache.commons.lang.StringUtils;
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
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.RunInQueue;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.trash.StorageTrash;
import org.zstack.core.trash.TrashType;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.SyncSystemTagFromVolumeMsg;
import org.zstack.header.message.*;
import org.zstack.header.storage.backup.AllocateBackupStorageMsg;
import org.zstack.header.storage.backup.AllocateBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.ReturnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint.ParamIn;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint.ParamOut;
import org.zstack.header.storage.snapshot.CreateTemplateFromVolumeSnapshotExtensionPoint.WorkflowTemplate;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus.StatusEvent;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostBackupFileDeletionMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.volume.*;
import org.zstack.longjob.LongJobUtils;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.FireSnapShotCanonicalEvent;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.message.OperationChecker;

import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotTreeBase {
    protected static OperationChecker allowedStatus = new OperationChecker(true);
    private static CLogger logger = Utils.getLogger(VolumeSnapshotTreeBase.class);

    @Autowired
    private StorageTrash trash;

    @Autowired
    private TagManager tagMgr;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;

    static {
        allowedStatus.addState(VolumeSnapshotStatus.Ready,
                VolumeSnapshotDeletionMsg.class.getName(),
                BackupVolumeSnapshotMsg.class.getName(),
                InstantiateDataVolumeFromVolumeSnapshotMsg.class.getName(),
                CreateTemplateFromVolumeSnapshotMsg.class.getName(),
                VolumeSnapshotBackupStorageDeletionMsg.class.getName(),
                APIRevertVolumeFromSnapshotMsg.class.getName(),
                APIRevertVmFromSnapshotGroupMsg.class.getName(),
                RevertVolumeFromSnapshotGroupMsg.class.getName(),
                RevertVolumeSnapshotMsg.class.getName(),
                APIDeleteVolumeSnapshotFromBackupStorageMsg.class.getName(),
                APIBackupVolumeSnapshotMsg.class.getName(),
                APIShrinkVolumeSnapshotMsg.class.getName()
        );

        allowedStatus.addState(VolumeSnapshotStatus.Deleting,
                VolumeSnapshotDeletionMsg.class.getName());

        allowedStatus.addState(VolumeSnapshotStatus.Creating,
                VolumeSnapshotDeletionMsg.class.getName());
    }

    protected VolumeSnapshotVO currentRoot;
    protected SnapshotLeaf currentLeaf;
    protected VolumeSnapshotTree fullTree;
    protected String syncSignature;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PrimaryStorageOverProvisioningManager psRaitoMgr;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    public VolumeSnapshotTreeBase(VolumeSnapshotVO vo, boolean syncOnVolume) {
        currentRoot = vo;
        if (syncOnVolume) {
            syncSignature = String.format("volume.snapshot.volume.%s", currentRoot.getVolumeUuid());
        } else {
            syncSignature = String.format("volume.snapshot.tree.%s", currentRoot.getTreeUuid());
        }
    }

    private ErrorCode isOperationAllowed(Message msg) {
        if (allowedStatus.isOperationAllowed(msg.getClass().getName(), currentRoot.getStatus().toString())) {
            return null;
        } else {
            return err(VolumeSnapshotErrors.NOT_IN_CORRECT_STATE,
                    "snapshot[uuid:%s, name:%s]'s status[%s] is not allowed for message[%s], allowed status%s",
                    currentRoot.getUuid(), currentRoot.getName(), currentRoot.getStatus(), msg.getClass().getName(), allowedStatus.getStatesForOperation(msg.getClass().getName()));
        }
    }

    private void refreshVO() {
        VolumeSnapshotVO vo = dbf.reload(currentRoot);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find volume snapshot[uuid:%s, name:%s], it may have been deleted by previous operation",
                    currentRoot.getUuid(), currentRoot.getName()));
        }

        currentRoot = vo;
        buildFullSnapshotTree();
        currentLeaf = fullTree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getUuid().equals(currentRoot.getUuid());
            }
        });
    }

    private VolumeSnapshotInventory getSelfInventory() {
        return VolumeSnapshotInventory.valueOf(currentRoot);
    }

    private void buildFullSnapshotTree() {
        List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.treeUuid, currentRoot.getTreeUuid())
                .list();

        fullTree = VolumeSnapshotTree.fromVOs(vos);
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateTemplateFromVolumeSnapshotMsg) {
            handle((CreateTemplateFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof CreateImageCacheFromVolumeSnapshotMsg) {
            handle((CreateImageCacheFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof InstantiateDataVolumeFromVolumeSnapshotMsg) {
            handle((InstantiateDataVolumeFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof VolumeSnapshotDeletionMsg) {
            handle((VolumeSnapshotDeletionMsg) msg);
        } else if (msg instanceof DeleteVolumeSnapshotMsg) {
            handle((DeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof RevertVolumeSnapshotMsg) {
            handle((RevertVolumeSnapshotMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotGroupMsg) {
            handle((RevertVolumeFromSnapshotGroupMsg) msg);
        } else if (msg instanceof CancelDeleteVolumeSnapshotMsg) {
            handle((CancelDeleteVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CancelDeleteVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "cancel-delete-volume-snapshot";
            }

            @Override
            public void run(SyncTaskChain chain) {
                CancelDeleteVolumeSnapshotReply reply = new CancelDeleteVolumeSnapshotReply();
                doCancelDeleteVolumeSnapshot(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
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

    private void doCancelDeleteVolumeSnapshot(CancelDeleteVolumeSnapshotMsg cmsg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("cancel-delete-volume-snapshot");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                CancelHostTasksMsg msg = new CancelHostTasksMsg();
                msg.setCancellationApiId(cmsg.getCancellationApiId());
                bus.makeLocalServiceId(msg, HostConstant.SERVICE_ID);
                bus.send(msg, new CloudBusCallBack(trigger) {
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
                completion.success();
            }
        }).start();
    }

    private void handle(RevertVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                RevertVolumeSnapshotReply reply = new RevertVolumeSnapshotReply();

                revert(msg, new Completion(msg, chain) {
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
                return String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid());
            }
        });
    }

    private void handle(DeleteVolumeSnapshotMsg msg) {
        DeleteVolumeSnapshotReply reply = new DeleteVolumeSnapshotReply();

        deleteVolumeSnapshot(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final VolumeSnapshotDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deletion(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    // TODO: BUG FIX, when deleting a volume the cascade extension will send messages to all snapshots
    // of this volume, which the oldest snapshot will delete descendant snapshots and set the volumeUuid
    // to NULL for all snapshots, so the after messages are useless
    private void deletion(final VolumeSnapshotDeletionMsg msg, final NoErrorCompletion completion) {
        final VolumeSnapshotDeletionReply reply = new VolumeSnapshotDeletionReply();

        try {
            refreshVO();
        } catch (OperationFailureException e) {
            // the snapshot has been deleted
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-snapshot-%s", currentRoot.getUuid()));
        chain.allowEmptyFlow();

        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        String primaryStorageType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, getSelfInventory().getPrimaryStorageUuid()).findValue();

    chain.then(new ShareFlow() {
        boolean ancestorOfLatest;
        long requiredSize;
        List<VolumeSnapshotInventory> memorySnapshotsOfDescendants = new ArrayList<>();

        @Override
        public void setup() {
        flow(new NoRollbackFlow() {
            String __name__ = "check-snapshot-reference";

            @Override
            public boolean skip(Map data) {
                return msg.isDbOnly();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> refVolUuids = VolumeSnapshotReferenceUtils.getAllReferenceVolumeUuids(currentLeaf);
                if (refVolUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                trigger.fail(operr("snapshot or its desendant has reference volume[uuids:%s]", refVolUuids));
            }
        });
        flow(new NoRollbackFlow() {
            String __name__ = "run snapshot protector";

            @Override
            public boolean skip(Map data) {
                return msg.isDbOnly();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (Platform.isSimulatorOn()) {
                    trigger.next();
                    return;
                }

                ErrorCodeList errList = new ErrorCodeList();
                new While<>(currentLeaf.getDescendants()).all((sp, whileCompletion) -> {
                    Optional<VolumeSnapshotDeletionProtector> protector = pluginRgty.getExtensionList(VolumeSnapshotDeletionProtector.class).stream().filter(p -> p.getPrimaryStorageType().equals(primaryStorageType))
                            .findFirst();
                    if (!protector.isPresent()) {
                        logger.warn(String.format("there are no protector for primary storage: %s", primaryStorageType));
                        whileCompletion.done();
                        return;
                    }

                    if (sp == null)  {
                        logger.warn(String.format("skip protector, got null descendant"));
                        whileCompletion.done();
                        return;
                    }
                    if (sp.getPrimaryStorageInstallPath() == null || sp.getVolumeUuid() == null) {
                        logger.warn(String.format("skip protector, descendant primary storage install path or volume uuid is null"));
                        whileCompletion.done();
                        return;
                    }

                    protector.get().protect(sp, new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(errList.getCauses().get(0));
                    }
                });
            }
        });

        if (Objects.equals(msg.getScope(), DeleteVolumeSnapshotScope.Chain.toString())) {
            if (msg.getScope() == null) {
                logger.warn("snapshot deletion scope is null, default to Chain scope");
            }

            long size = 0;
            for (VolumeSnapshotInventory inv : currentLeaf.getDescendants()) {
                if (inv.isLatest()) {
                    ancestorOfLatest = true;
                }
                size += inv.getSize();
            }
            requiredSize = Math.min(size, volume.getSize());

            deleteChainFlows();
        } else {
            deleteSingleFlows();
        }

        done();
        error();
        }

    private void deleteChainFlows() {
        flow(new Flow() {
            String __name__ = String.format("change-volume-snapshot-status-%s", VolumeSnapshotStatus.Deleting);

            public void run(final FlowTrigger trigger, Map data) {
                changeStatusOfSnapshots(StatusEvent.delete, currentLeaf.getDescendants(), new Completion(trigger) {
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

            @Override
            public void rollback(final FlowRollback trigger, Map data) {
                changeStatusOfSnapshots(StatusEvent.ready, currentLeaf.getDescendants(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }
        });

        if (!msg.isVolumeDeletion()) {
            // this deletion is caused by snapshot deletion, check if merge need
            Boolean onCurrentTree = Q.New(VolumeSnapshotTreeVO.class)
                    .select(VolumeSnapshotTreeVO_.current)
                    .eq(VolumeSnapshotTreeVO_.uuid, currentRoot.getTreeUuid())
                    .findValue();

            boolean needMerge = onCurrentTree && ancestorOfLatest && currentRoot.getPrimaryStorageUuid() != null && VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString().equals(currentRoot.getType());
            if (needMerge) {
                flow(new Flow() {
                    String __name__ = "allocate-primary-storage";

                    boolean success;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                        for (SnapshotDeletionExtensionPoint ext : pluginRgty.getExtensionList(SnapshotDeletionExtensionPoint.class)) {
                            String hostUuid = ext.getHostUuidByResourceUuid(currentRoot.getPrimaryStorageUuid() ,currentRoot.getVolumeUuid());
                            amsg.setRequiredHostUuid(hostUuid);
                        }
                        amsg.setRequiredPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        amsg.setSize(requiredSize);
                        amsg.setNoOverProvisioning(true);
                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    success = true;
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                            imsg.setDiskSize(requiredSize);
                            imsg.setNoOverProvisioning(true);
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, currentRoot .getPrimaryStorageUuid());
                            bus.send(imsg);
                        }

                        trigger.rollback();
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = "merge-volume-snapshots-to-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        MergeVolumeSnapshotOnPrimaryStorageMsg mmsg = new MergeVolumeSnapshotOnPrimaryStorageMsg();
                        VolumeSnapshotInventory from = currentLeaf.getParent() == null ? currentLeaf.getInventory() : currentLeaf.getParent().getInventory();
                        mmsg.setFrom(from);
                        VolumeVO vol = dbf.findByUuid(currentRoot.getVolumeUuid(), VolumeVO.class);
                        mmsg.setTo(VolumeInventory.valueOf(vol));
                        mmsg.setFullRebase(currentLeaf.getParent() == null);
                        bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(mmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                                imsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                                imsg.setDiskSize(requiredSize);
                                imsg.setNoOverProvisioning(true);
                                bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                                bus.send(imsg);
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });
            }

            flow(new NoRollbackFlow() {
                String __name__ = "delete-volume-snapshots-from-backup-storage";

                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    final List<VolumeSnapshotBackupStorageDeletionMsg> dmsgs = makeVolumeSnapshotBackupStorageDeletionMsg(null);
                    if (dmsgs.isEmpty()) {
                        trigger.next();
                        return;
                    }

                    bus.send(dmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_DELETE_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                        @Override
                        public void run(List<MessageReply> replies) {
                            for (MessageReply r : replies) {
                                if (!r.isSuccess()) {
                                    VolumeSnapshotBackupStorageDeletionMsg dmsg = dmsgs.get(replies.indexOf(r));
                                    logger.warn(String.format("failed to delete snapshot[uuid:%s] on backup storage[uuids: %s], the backup storage should cleanup",
                                            dmsg.getSnapshotUuid(), dmsg.getBackupStorageUuids()));
                                }
                            }

                            trigger.next();
                        }
                    });
                }
            });
        }

        flow(new NoRollbackFlow() {
            String __name__ = "delete-volume-snapshots-from-primary-storage";

            @Override
            public boolean skip(Map data) {
                return msg.isDbOnly();
            }

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<VolumeSnapshotInventory> snapshotsToDelete = new ArrayList<>(currentLeaf.getDescendants());
                memorySnapshotsOfDescendants = getMemorySnapshotsOfDescendants();
                snapshotsToDelete.addAll(memorySnapshotsOfDescendants);
                if (Objects.equals(currentLeaf.getInventory().getVolumeType(), VolumeType.Root.toString())) {
                    snapshotsToDelete.addAll(getMemorySnapshotsOfDescendants());
                }
                final List<VolumeSnapshotPrimaryStorageDeletionMsg> pmsgs = transformAndRemoveNull(snapshotsToDelete, arg -> {
                    if (arg.getPrimaryStorageUuid() == null) {
                        return null;
                    }

                    VolumeSnapshotPrimaryStorageDeletionMsg pmsg = new VolumeSnapshotPrimaryStorageDeletionMsg();
                    pmsg.setUuid(arg.getUuid());
                    pmsg.setVolumeDelete(msg.isVolumeDeletion());
                    bus.makeTargetServiceIdByResourceUuid(pmsg, VolumeSnapshotConstant.SERVICE_ID, arg.getPrimaryStorageUuid());
                    return pmsg;
                });

                if (pmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                bus.send(pmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_DELETE_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        ErrorCodeList errors = new ErrorCodeList();
                        for (MessageReply r : replies) {
                            if (!r.isSuccess()) {
                                VolumeSnapshotPrimaryStorageDeletionMsg pmsg = pmsgs.get(replies.indexOf(r));
                                if (!msg.isVolumeDeletion() && VolumeSnapshotErrors.FULL_SNAPSHOT_ERROR.toString().equals(r.getError().getCode())) {
                                    errors.getCauses().add(r.getError());
                                } else {
                                    logger.warn(String.format("failed to delete snapshot[uuid:%s] on primary storage[uuid:%s] cause: [%s], the primary storage should cleanup",
                                            pmsg.getSnapshotUuid(), currentRoot.getPrimaryStorageUuid(), r.getError().getDetails()));
                                }
                            }
                        }
                        if (errors.getCauses().size() > 0) {
                            trigger.fail(multiErr(errors));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            private List<VolumeSnapshotInventory> getMemorySnapshotsOfDescendants() {
                List<String> groupUuids = currentLeaf.getDescendants().stream()
                        .filter(inv -> !Objects.equals(inv.getUuid(), currentLeaf.getUuid()))
                        .map(VolumeSnapshotInventory::getGroupUuid).filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (groupUuids.isEmpty()) {
                    return new ArrayList<>();
                }

                List<String> memorySnapshotUuids = Q.New(VolumeSnapshotGroupRefVO.class)
                        .in(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuids)
                        .eq(VolumeSnapshotGroupRefVO_.volumeType, VolumeType.Memory.toString())
                        .eq(VolumeSnapshotGroupRefVO_.snapshotDeleted, false)
                        .select(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid).listValues();
                if (memorySnapshotUuids.isEmpty()) {
                    return new ArrayList<>();
                }

                List<VolumeSnapshotVO> memorySnapshots = Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, memorySnapshotUuids).list();
                if (memorySnapshots.isEmpty()) {
                    return new ArrayList<>();
                }
                return VolumeSnapshotInventory.valueOf(memorySnapshots);
            }
        });
    }

    private void done() {
        done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                if (Objects.equals(msg.getScope(), DeleteVolumeSnapshotScope.Single.toString())) {
                    bus.reply(msg, reply);
                    completion.done();
                    return;
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        if (msg.isVolumeDeletion()) {
                            sql("update VolumeSnapshotTreeVO tree set tree.volumeUuid = NULL where tree.uuid = :treeUuid")
                                    .param("treeUuid", currentRoot.getTreeUuid()).execute();

                            sql("update VolumeSnapshotVO s set s.volumeUuid = NULL where s.treeUuid = :treeUuid")
                                    .param("treeUuid", currentRoot.getTreeUuid()).execute();
                        }


                        if (!msg.isVolumeDeletion() && ancestorOfLatest && currentRoot.getParentUuid() != null) {
                            // reset latest
                            sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, currentRoot.getParentUuid())
                                    .set(VolumeSnapshotVO_.latest, true).update();
                            logger.debug(String.format("reset latest snapshot of tree[uuid:%s] to snapshot[uuid:%s]",
                                    currentRoot.getTreeUuid(), currentRoot.getParentUuid()));
                        }
                    }
                }.execute();

                if (!cleanup()) {
                    changeStatusOfSnapshots(StatusEvent.ready, currentLeaf.getDescendants(), new Completion(msg, completion) {
                        @Override
                        public void success() {
                            bus.reply(msg, reply);
                            completion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            completion.done();
                        }
                    });
                } else {
                    if (msg.isVolumeDeletion()) {
                        List<String> aliveAncestors = currentLeaf.getAncestors().stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toList());
                        if (!aliveAncestors.isEmpty()) {
                            SQL.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, currentRoot.getTreeUuid())
                                    .set(VolumeSnapshotTreeVO_.volumeUuid, currentRoot.getVolumeUuid())
                                    .update();
                            SQL.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, aliveAncestors)
                                    .set(VolumeSnapshotVO_.volumeUuid, currentRoot.getVolumeUuid())
                                    .update();
                        }
                    }

                    cleanUpMemorySnapshots();
                    bus.reply(msg, reply);
                    completion.done();
                }
            }

            private void cleanUpMemorySnapshots() {
                if (!Objects.equals(currentLeaf.getInventory().getVolumeType(), VolumeType.Root.toString())) {
                    logger.debug("skipping cleanup: current snapshot is not a root volume snapshot");
                    return;
                }
                if (memorySnapshotsOfDescendants.isEmpty()) {
                    logger.debug("no descendant memory snapshots found. cleanup completed");
                    return;
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        List<String> memorySnapshotUuids = memorySnapshotsOfDescendants.stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toList());
                        List<String> memorySnapshotTreeUuids = memorySnapshotsOfDescendants.stream().map(VolumeSnapshotInventory::getTreeUuid).collect(Collectors.toList());
                        sql(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, memorySnapshotUuids).hardDelete();
                        sql(VolumeSnapshotTreeVO.class).in(VolumeSnapshotTreeVO_.uuid, memorySnapshotTreeUuids).hardDelete();
                        logger.debug("success to cleanup descendant memory snapshots");
                    }
                }.execute();
            }
        });
    }

    private void error() {
        error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void deleteSingleFlows() {
        flow(new NoRollbackFlow() {
            VmInstanceState vmState;
            String srcSnapshotParentPath;

            String __name__ = "delete-single-volume-snapshot";

            public void run(final FlowTrigger trigger, Map data) {
                if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(currentRoot.getType())
                        || Objects.equals(currentRoot.getVolumeType(), VolumeType.Memory.toString())) {
                    deleteVolumeSnapshotAndSyncVolumeSize(new Completion(completion) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                    return;
                }

                if (volume.getVmInstanceUuid() != null) {
                    vmState = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volume.getVmInstanceUuid()).select(VmInstanceVO_.state).findValue();
                    if (vmState != VmInstanceState.Running && vmState != VmInstanceState.Paused
                            && vmState != VmInstanceState.Destroyed && vmState != VmInstanceState.Stopped && vmState != VmInstanceState.Destroying) {
                        trigger.fail(operr("vm[uuid:%s] is not Running, Paused or Destroyed, Stopped, Destroying, " +
                                "current state[%s]", volume.getVmInstanceUuid(), vmState));
                        return;
                    }
                }

                stepDelete(new Completion(msg, completion) {
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

            private void stepDelete(Completion completion) {
                List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, currentRoot.getTreeUuid()).list();
                boolean current = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, currentRoot.getTreeUuid())
                        .select(VolumeSnapshotTreeVO_.current).findValue();
                VolumeTree volumeTree = VolumeTree.fromVOs(vos, current, VolumeInventory.valueOf(volume));
                List<VolumeTree.VolumeSnapshotLeaf> children = volumeTree.getSnapshotLeaf(currentRoot.getUuid()).getChildren();

                if (children.isEmpty()) {
                    deleteVolumeSnapshotAndSyncVolumeSize(completion);
                    return;
                }

                VolumeTree.VolumeSnapshotLeaf onlineChild = children.stream()
                        .filter(child -> volumeTree.isOnline(current, currentRoot.getUuid(), child.getUuid(), vmState))
                        .findFirst().orElse(null);

                Completion comp = new Completion(completion) {
                    @Override
                    public void success() {
                        stepDelete(completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                };
                VolumeTree.VolumeSnapshotLeaf child = children.get(0);
                if (children.size() == 1) {
                    DeleteVolumeSnapshotDirection direction = volumeTree.resolveDirection(currentRoot.getUuid(), child.getUuid(),
                            msg.getDirection(), currentRoot.isLatest(), vmState);
                    boolean online = volumeTree.isOnline(current, currentRoot.getUuid(), child.getUuid(), vmState);
                    if (Objects.equals(direction, DeleteVolumeSnapshotDirection.Commit)) {
                        commit(child, volumeTree, online, comp);
                    } else {
                        pull(child, volumeTree, online, comp);
                    }
                } else {
                    if (onlineChild != null && Objects.equals(child.getUuid(), onlineChild.getUuid())) {
                        child = children.get(1);
                    }
                    boolean online = volumeTree.isOnline(current, currentRoot.getUuid(), child.getUuid(), vmState);
                    pull(child, volumeTree, online, comp);
                }
            }

            private void commit(VolumeTree.VolumeSnapshotLeaf child, VolumeTree volumeTree, boolean online, Completion completion) {
                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName(String.format("delete-single-volume-snapshot-%s-by-commit", currentRoot.getUuid()));
                chain.then(new ShareFlow() {
                    final VolumeSnapshotInventory srcSnapshotInv = child.getInventory();
                    final VolumeTree.VolumeSnapshotLeaf srcSnapshotLeaf = child;
                    final VolumeSnapshotInventory dstSnapshotInv = VolumeSnapshotInventory.valueOf(currentRoot);
                    long size = srcSnapshotInv.getSize();
                    String allocatedInstall;

                    @Override
                    public void setup() {
                        if (Objects.equals(srcSnapshotInv.getUuid(), volume.getUuid())) {
                            flow(new NoRollbackFlow() {
                                String __name__ = String.format("get-volume-%s-current-size", volume.getUuid());

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();
                                    smsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                    smsg.setVolumeUuid(volume.getUuid());
                                    smsg.setInstallPath(volume.getInstallPath());
                                    bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                    bus.send(smsg, new CloudBusCallBack(completion) {
                                        @Override
                                        public void run(MessageReply reply) {
                                            if (!reply.isSuccess()) {
                                                trigger.fail(operr(String.format("failed to get volume[uuid:%s, installPath:%s] size " +
                                                                "on primary storage[uuid:%s], %s", volume.getUuid(), volume.getInstallPath(),
                                                        volume.getPrimaryStorageUuid(), reply.getError().getDetails())));
                                                return;
                                            }

                                            SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                                            size = r.getActualSize();
                                            srcSnapshotInv.setSize(size);
                                            trigger.next();
                                        }
                                    });
                                }
                            });
                        }

                        flow(new Flow() {
                            String __name__ = "allocate-primary-storage-capacity";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                                amsg.setRequiredPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                amsg.setSize(size);
                                amsg.setNoOverProvisioning(true);
                                amsg.setRequiredInstallUri(String.format("volume://%s", volume.getUuid()));
                                bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }
                                        AllocatePrimaryStorageSpaceReply r = reply.castReply();
                                        allocatedInstall = r.getAllocatedInstallUrl();
                                        trigger.next();
                                    }
                                });
                            }

                            @Override
                            public void rollback(FlowRollback trigger, Map data) {
                                if (allocatedInstall == null) {
                                    trigger.rollback();
                                    return;
                                }
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setDiskSize(size);
                                rmsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                rmsg.setNoOverProvisioning(true);
                                rmsg.setAllocatedInstallUrl(allocatedInstall);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                bus.send(rmsg);
                                trigger.rollback();
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "commit-volume-snapshot-on-primary-storage";

                            long newInstallPathSize;

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                List<String> childrenInstallPath = child.getChildren().stream().map(c ->
                                        c.getInventory().getPrimaryStorageInstallPath()).collect(Collectors.toList());

                                if (online) {
                                    String hostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volume.getVmInstanceUuid())
                                            .select(VmInstanceVO_.hostUuid).findValue();
                                    CommitVolumeSnapshotOnHypervisorMsg cmsg = new CommitVolumeSnapshotOnHypervisorMsg();
                                    cmsg.setHostUuid(hostUuid);
                                    cmsg.setVolume(VolumeInventory.valueOf(volume));
                                    cmsg.setSrcSnapshot(srcSnapshotInv);
                                    cmsg.setDstSnapshot(dstSnapshotInv);
                                    cmsg.setSrcChildrenInstallPathInDb(childrenInstallPath);
                                    bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, hostUuid);
                                    bus.send(cmsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply r) {
                                            if (!r.isSuccess()) {
                                                trigger.fail(r.getError());
                                                return;
                                            }
                                            CommitVolumeSnapshotOnHypervisorReply re = r.castReply();
                                            newInstallPathSize = re.getSize();
                                            updateDatabase();
                                            trigger.next();
                                        }
                                    });
                                } else {
                                    CommitVolumeSnapshotOnPrimaryStorageMsg cmsg = new CommitVolumeSnapshotOnPrimaryStorageMsg();
                                    cmsg.setVolume(VolumeInventory.valueOf(volume));
                                    cmsg.setSrcSnapshot(srcSnapshotInv);
                                    cmsg.setDstSnapshot(dstSnapshotInv);
                                    cmsg.setSrcChildrenInstallPathInDb(childrenInstallPath);
                                    bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                    bus.send(cmsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply r) {
                                            if (!r.isSuccess()) {
                                                trigger.fail(r.getError());
                                                return;
                                            }
                                            CommitVolumeSnapshotOnPrimaryStorageReply re = r.castReply();
                                            newInstallPathSize = re.getSize();
                                            updateDatabase();
                                            trigger.next();
                                        }
                                    });
                                }
                            }

                            private void updateDatabase() {
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setDiskSize(size);
                                rmsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                rmsg.setAllocatedInstallUrl(allocatedInstall);
                                rmsg.setNoOverProvisioning(true);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                bus.send(rmsg);

                                if (Objects.equals(srcSnapshotInv.getUuid(), volume.getUuid())) {
                                    markSnapshotAsVolume(VolumeInventory.valueOf(volume), size, dstSnapshotInv.getPrimaryStorageInstallPath(), newInstallPathSize);
                                    return;
                                }

                                volumeTree.updateDatabaseAfterCommit(srcSnapshotLeaf, dstSnapshotInv, newInstallPathSize);
                            }
                        });

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

            private void pull(VolumeTree.VolumeSnapshotLeaf child, VolumeTree volumeTree, boolean online, Completion completion) {
                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName(String.format("delete-single-volume-snapshot-%s-by-pull", currentRoot.getUuid()));
                chain.then(new ShareFlow() {
                    final VolumeSnapshotInventory srcSnapshotInv = VolumeSnapshotInventory.valueOf(currentRoot);
                    final VolumeSnapshotInventory dstSnapshotInv = child.getInventory();
                    final VolumeTree.VolumeSnapshotLeaf dstSnapshotLeaf = child;
                    String hostUuid;
                    long newInstallPathSize;
                    String allocatedInstall;

                    @Override
                    public void setup() {
                        if (volume.getVmInstanceUuid() != null && (vmState == VmInstanceState.Running || vmState == VmInstanceState.Paused)) {
                            hostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volume.getVmInstanceUuid())
                                    .select(VmInstanceVO_.hostUuid).findValue();
                        }

                        flow(new NoRollbackFlow() {
                            String __name__ = "get-snapshot-backing-chain";

                            @Override
                            public boolean skip(Map data) {
                                return srcSnapshotParentPath != null;
                            }

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                GetVolumeBackingChainFromPrimaryStorageMsg gmsg = new GetVolumeBackingChainFromPrimaryStorageMsg();
                                gmsg.setVolumeUuid(volume.getUuid());
                                gmsg.setRootInstallPaths(Collections.singletonList(srcSnapshotInv.getPrimaryStorageInstallPath()));
                                gmsg.setPrimaryStorageUuid(srcSnapshotInv.getPrimaryStorageUuid());
                                gmsg.setHostUuid(hostUuid);
                                gmsg.setVolumeFormat(volume.getFormat());
                                bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, gmsg.getPrimaryStorageUuid());
                                bus.send(gmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }

                                        GetVolumeBackingChainFromPrimaryStorageReply gr = reply.castReply();
                                        List<String> backingChainInstallPath = gr.getBackingChainInstallPath().get(srcSnapshotInv.getPrimaryStorageInstallPath());
                                        if (!backingChainInstallPath.isEmpty()) {
                                            srcSnapshotParentPath = backingChainInstallPath.get(0);
                                        }
                                        trigger.next();
                                    }
                                });
                            }
                        });

                        flow(new Flow() {
                            String __name__ = "allocate-primary-storage-capacity";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                                amsg.setRequiredPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                amsg.setSize(srcSnapshotInv.getSize());
                                amsg.setNoOverProvisioning(true);
                                amsg.setRequiredInstallUri(String.format("volume://%s", volume.getUuid()));
                                bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }
                                        AllocatePrimaryStorageSpaceReply r = reply.castReply();
                                        allocatedInstall = r.getAllocatedInstallUrl();
                                        trigger.next();
                                    }
                                });
                            }

                            @Override
                            public void rollback(FlowRollback trigger, Map data) {
                                if (allocatedInstall == null) {
                                    trigger.rollback();
                                    return;
                                }
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setDiskSize(srcSnapshotInv.getSize());
                                rmsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                rmsg.setNoOverProvisioning(true);
                                rmsg.setAllocatedInstallUrl(allocatedInstall);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                bus.send(rmsg);
                                trigger.rollback();
                            }
                        });

                        if (Objects.equals(dstSnapshotInv.getUuid(), volume.getUuid())) {
                            flow(new NoRollbackFlow() {
                                String __name__ = String.format("get-volume-%s-current-size", volume.getUuid());

                                @Override
                                public void run(FlowTrigger trigger, Map data) {
                                    SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();
                                    smsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                    smsg.setVolumeUuid(volume.getUuid());
                                    smsg.setInstallPath(volume.getInstallPath());
                                    bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                    bus.send(smsg, new CloudBusCallBack(completion) {
                                        @Override
                                        public void run(MessageReply reply) {
                                            if (!reply.isSuccess()) {
                                                trigger.fail(operr(String.format("failed to get volume[uuid:%s, installPath:%s] size on primary storage[uuid:%s], because %s",
                                                        volume.getUuid(), volume.getInstallPath(), volume.getPrimaryStorageUuid(), reply.getError().getDetails())));
                                                return;
                                            }

                                            SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                                            dstSnapshotInv.setSize(r.getActualSize());
                                            trigger.next();
                                        }
                                    });
                                }
                            });
                        }

                        flow(new NoRollbackFlow() {
                            String __name__ = "pull-volume-snapshot-on-primary-storage";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                if (online) {
                                    PullVolumeSnapshotOnHypervisorMsg pmsg = new PullVolumeSnapshotOnHypervisorMsg();
                                    pmsg.setHostUuid(hostUuid);
                                    pmsg.setVolume(VolumeInventory.valueOf(volume));
                                    pmsg.setSrcSnapshotParentPath(srcSnapshotParentPath);
                                    pmsg.setSrcSnapshot(srcSnapshotInv);
                                    pmsg.setDstSnapshot(dstSnapshotInv);
                                    bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, hostUuid);
                                    bus.send(pmsg, new CloudBusCallBack(completion) {
                                        @Override
                                        public void run(MessageReply r) {
                                            if (!r.isSuccess()) {
                                                trigger.fail(r.getError());
                                                return;
                                            }

                                            PullVolumeSnapshotOnHypervisorReply re = r.castReply();
                                            newInstallPathSize = re.getSize();
                                            updateDatabase();
                                            trigger.next();
                                        }
                                    });
                                } else {
                                    PullVolumeSnapshotOnPrimaryStorageMsg pmsg = new PullVolumeSnapshotOnPrimaryStorageMsg();
                                    pmsg.setVolume(VolumeInventory.valueOf(volume));
                                    pmsg.setSrcSnapshotParentPath(srcSnapshotParentPath);
                                    pmsg.setSrcSnapshot(srcSnapshotInv);
                                    pmsg.setDstSnapshot(dstSnapshotInv);
                                    bus.makeTargetServiceIdByResourceUuid(pmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                    bus.send(pmsg, new CloudBusCallBack(trigger) {
                                        @Override
                                        public void run(MessageReply r) {
                                            if (!r.isSuccess()) {
                                                trigger.fail(r.getError());
                                                return;
                                            }
                                            PullVolumeSnapshotOnPrimaryStorageReply re = r.castReply();
                                            newInstallPathSize = re.getSize();
                                            updateDatabase();
                                            trigger.next();
                                        }
                                    });
                                }
                            }

                            private void updateDatabase() {
                                ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                                rmsg.setDiskSize(dstSnapshotInv.getSize());
                                rmsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                                rmsg.setAllocatedInstallUrl(allocatedInstall);
                                rmsg.setNoOverProvisioning(true);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                                bus.send(rmsg);

                                if (Objects.equals(dstSnapshotInv.getUuid(), volume.getUuid())) {
                                    volumeTree.updateDatabaseAfterPullToVolume(srcSnapshotInv);
                                    return;
                                }
                                volumeTree.updateDatabaseAfterPull(srcSnapshotInv, dstSnapshotLeaf, newInstallPathSize);
                            }
                        });

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

            private void deleteVolumeSnapshotAndSyncVolumeSize(Completion completion) {
                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName("delete-volume-snapshot-and-sync-volume-size");
                chain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        flow(new NoRollbackFlow() {
                            String __name__ = String.format("delete-snapshot-%s-on-primary-storage", currentRoot.getUuid());

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                VolumeSnapshotPrimaryStorageDeletionMsg pmsg = new VolumeSnapshotPrimaryStorageDeletionMsg();
                                pmsg.setUuid(currentRoot.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(pmsg, VolumeSnapshotConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                                bus.send(pmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            //TODO add gc
                                            logger.warn(String.format("failed to delete snapshot[uuid:%s] on primary storage[uuid:%s], " +
                                                    "%s", currentRoot.getUuid(), currentRoot.getPrimaryStorageUuid(), reply.getError().getDetails()));
                                        }
                                        VolumeSnapshotVO vo = dbf.findByUuid(currentRoot.getUuid(), VolumeSnapshotVO.class);
                                        cleanupInQueue(VolumeSnapshotInventory.valueOf(vo), new NoErrorCompletion() {
                                            @Override
                                            public void done() {
                                                trigger.next();
                                            }
                                        });
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = String.format("sync-volume-%s-size", volume.getUuid());

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                                msg.setVolumeUuid(volume.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                                bus.send(msg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (!reply.isSuccess()) {
                                            logger.warn(String.format("failed to sync volume[%s] size, because %s", volume.getUuid(), reply.getError()));
                                        }
                                        trigger.next();
                                    }
                                });
                            }
                        });

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
        });
    }
        }).start();
    }

    private void cleanupInQueue(VolumeSnapshotInventory volumeSnapshotInv, NoErrorCompletion completion) {
        if (volumeSnapshotInv.getGroupUuid() == null) {
            cleanupAfterDeleteSingleSnapshot(volumeSnapshotInv, new NoErrorCompletion() {
                @Override
                public void done() {
                    completion.done();
                }
            });
            return;
        }

        new RunInQueue(String.format("cleanup-snapshot-on-volumeSnapshotGroup-%s", volumeSnapshotInv.getGroupUuid()), thdf, 1)
                .name("cleanup-snapshot-on-volumeSnapshotGroup-in-queue").asyncBackup(completion)
                .run(chain -> cleanupAfterDeleteSingleSnapshot(volumeSnapshotInv, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        completion.done();
                        chain.next();
                    }
                }));
    }

    private void cleanupAfterDeleteSingleSnapshot(VolumeSnapshotInventory volumeSnapshotInv, final NoErrorCompletion completion) {
        boolean cleanup = new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, volumeSnapshotInv.getUuid()).hardDelete();
                if (!q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, volumeSnapshotInv.getTreeUuid()).isExists()) {
                    logger.debug(String.format("volume snapshot tree[uuid:%s] has no leaf, delete it", volumeSnapshotInv.getTreeUuid()));
                    sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, volumeSnapshotInv.getTreeUuid()).hardDelete();
                }
                return true;
            }
        }.execute();

        if (cleanup) {
            ungroupAfterDeleteSingleSnapshot(volumeSnapshotInv);
        }

        completion.done();
    }

    private void ungroupAfterDeleteSingleSnapshot(VolumeSnapshotInventory volumeSnapshotInv) {
        if (volumeSnapshotInv.getGroupUuid() == null) {
            return;
        }

        SQL.New(VolumeSnapshotGroupRefVO.class).eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, volumeSnapshotInv.getUuid())
                .set(VolumeSnapshotGroupRefVO_.snapshotDeleted, true).update();
        long count = Q.New(VolumeSnapshotGroupRefVO.class)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, volumeSnapshotInv.getGroupUuid())
                .eq(VolumeSnapshotGroupRefVO_.snapshotDeleted, false).count();
        if (count == 0) {
            cleanVmHostBackupFilesForGroup(list(volumeSnapshotInv.getGroupUuid()));
            dbf.removeByPrimaryKey(volumeSnapshotInv.getGroupUuid(), VolumeSnapshotGroupVO.class);
            logger.debug(String.format("snapshot group[uuid:%s] all volume snapshot has been deleted, " +
                    "delete snapshot group", volumeSnapshotInv.getGroupUuid()));
        }
    }

    private void handle(final InstantiateDataVolumeFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createDataVolume(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-data-volume-from-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void createDataVolume(final InstantiateDataVolumeFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final InstantiateDataVolumeFromVolumeSnapshotReply reply = new InstantiateDataVolumeFromVolumeSnapshotReply();

        refreshVO();
        ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-data-volume-from-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            String installPath;
            long size;
            long actualSize;
            String protocol;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-data-volume-on-primary-storage";

                    boolean incremental;

                    public void run(final FlowTrigger trigger, Map data) {
                        CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg();
                        cmsg.setSnapshot(currentLeaf.getInventory());
                        cmsg.setVolumeUuid(msg.getVolume().getUuid());
                        cmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        cmsg.setSystemTags(msg.getSystemTags());
                        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply cr = reply.castReply();
                                installPath = cr.getInstallPath();
                                actualSize = cr.getActualSize();
                                size = cr.getSize();
                                protocol = cr.getProtocol();
                                if (cr.isIncremental()) {
                                    incremental = true;
                                    msg.getVolume().setInstallPath(installPath);
                                    VolumeSnapshotReferenceUtils.buildSnapshotReferenceForNewVolume(msg.getVolume(), currentRoot, null);
                                }

                                //update volume install path for ps allocate
                                //TODO: remove this
                                SQL.New(VolumeVO.class).set(VolumeVO_.installPath, installPath).eq(VolumeVO_.uuid, msg.getVolume().getUuid()).update();
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (installPath != null) {
                            DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                            dmsg.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(getSelfInventory().getFormat()).toString());
                            dmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                            dmsg.setInstallPath(installPath);
                            dmsg.setBitsType(VolumeVO.class.getSimpleName());
                            dmsg.setBitsUuid(msg.getVolume().getUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, dmsg.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        }

                        if (incremental) {
                            VolumeSnapshotReferenceUtils.rollbackSnapshotReferenceForNewVolume(msg.getVolume().getUuid());
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-primary-storage";

                    boolean success;
                    String allocatedInstall;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        amsg.setSize(size);
                        amsg.setRequiredInstallUri(String.format("volume://%s", msg.getVolume().getUuid()));

                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstall = ar.getAllocatedInstallUrl();
                                success = true;
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                            rmsg.setDiskSize(size);
                            rmsg.setAllocatedInstallUrl(allocatedInstall);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
                            bus.send(rmsg);
                        }
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        VolumeInventory inv = msg.getVolume();
                        inv.setInstallPath(installPath);
                        inv.setSize(size);
                        inv.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
                        inv.setFormat(currentRoot.getFormat());
                        inv.setProtocol(protocol);
                        reply.setActualSize(actualSize);
                        reply.setInventory(inv);
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

    private void handle(final CreateTemplateFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createTemplate(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-template-from-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void handle(CreateImageCacheFromVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                createImageCache(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("create-image-cache-from-volume-snapshot-%s", currentRoot.getUuid());
            }
        });
    }

    private void changeStatusOfSnapshots(final StatusEvent evt, final List<VolumeSnapshotInventory> snapshots, final Completion completion) {
        List<ChangeVolumeSnapshotStatusMsg> msgs = transformAndRemoveNull(snapshots, arg -> {
            ChangeVolumeSnapshotStatusMsg msg = new ChangeVolumeSnapshotStatusMsg();
            msg.setEvent(evt.toString());
            msg.setSnapshotUuid(arg.getUuid());
            bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
            return msg;
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                ErrorCode err = null;
                VolumeSnapshotInventory failSnapshot = null;
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        err = r.getError();
                        failSnapshot = snapshots.get(replies.indexOf(r));
                        break;
                    }
                }

                if (err != null) {
                    completion.fail(operr("failed to change status of volume snapshot[uuid:%s, name:%s] by status event[%s]",
                                    failSnapshot.getUuid(), failSnapshot.getName(), evt).causedBy(err));
                } else {
                    completion.success();
                }
            }
        });
    }

    private void createTemplate(final CreateTemplateFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final CreateTemplateFromVolumeSnapshotReply reply = new CreateTemplateFromVolumeSnapshotReply();

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            reply.setError(err);
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        String psType = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, currentRoot.getPrimaryStorageUuid())
                .findValue();

        CreateTemplateFromVolumeSnapshotExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, CreateTemplateFromVolumeSnapshotExtensionPoint.class);
        if (ext == null) {
            throw new CloudRuntimeException(String.format("the primary storage[type:%s] doesn't implement CreateTemplateFromVolumeSnapshotExtensionPoint", psType));
        }

        ParamIn paramIn = new ParamIn();
        paramIn.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
        paramIn.setSnapshot(currentLeaf.getInventory());
        paramIn.setImage(ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class)));
        if (msg.getEncrypted() != null) {
            paramIn.getSnapshot().setEncrypted(msg.getEncrypted());
        }
        WorkflowTemplate workflowTemplate = ext.createTemplateFromVolumeSnapshot(paramIn);

        ParamOut paramOut = new ParamOut();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-template-from-snapshot-%s-%s", currentLeaf.getInventory().getName(),
                currentLeaf.getUuid()));
        chain.putData(
                e(ParamIn.class, paramIn),
                e(ParamOut.class, paramOut)
        );
        chain.then(workflowTemplate.getCreateTemporaryTemplate());
        chain.preCheck(data -> LongJobUtils.buildErrIfCanceled());
        chain.then(new Flow() {
            String __name__ = "allocate-backup-storage";

            String allocateBackupStorageUuid;
            Long allocatedSize;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ParamOut out = (ParamOut) data.get(ParamOut.class);
                allocatedSize = out.getActualSize();
                AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                amsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                amsg.setSize(allocatedSize);

                if (amsg.getBackupStorageUuid() == null) {
                    String volumePsUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid())
                            .select(VolumeVO_.primaryStorageUuid)
                            .findValue();
                    amsg.setRequiredPrimaryStorageUuid(volumePsUuid);
                }

                bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                bus.send(amsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            AllocateBackupStorageReply r = reply.castReply();
                            allocateBackupStorageUuid = r.getInventory().getUuid();
                            paramIn.setBackupStorageUuid(allocateBackupStorageUuid);
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (allocateBackupStorageUuid != null) {
                    ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                    rmsg.setSize(allocatedSize);
                    rmsg.setBackupStorageUuid(allocateBackupStorageUuid);
                    bus.makeLocalServiceId(rmsg, BackupStorageConstant.SERVICE_ID);
                    bus.send(rmsg);
                }

                trigger.rollback();
            }
        });
        chain.then(workflowTemplate.getUploadToBackupStorage());
        chain.then(workflowTemplate.getDeleteTemporaryTemplate());
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                SyncSystemTagFromVolumeMsg smsg = new SyncSystemTagFromVolumeMsg();
                smsg.setImageUuid(msg.getImageUuid());
                smsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeLocalServiceId(smsg, ImageConstant.SERVICE_ID);
                bus.send(smsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("sync image[uuid:%s]system tag fail", msg.getImageUuid()));
                        }
                        trigger.next();
                    }
                });
            }
        });
        chain.done(new FlowDoneHandler(msg, completion) {
            @Override
            public void handle(Map data) {
                ParamOut out = (ParamOut) data.get(ParamOut.class);
                reply.setActualSize(out.getActualSize());
                reply.setSize(out.getSize());
                reply.setBackupStorageInstallPath(out.getBackupStorageInstallPath());
                reply.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg, completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).Finally(new FlowFinallyHandler(msg, completion) {
            @Override
            public void Finally() {
                completion.done();
            }
        }).start();
    }

    private void createImageCache(final CreateImageCacheFromVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        CreateImageCacheFromVolumeSnapshotReply reply = new CreateImageCacheFromVolumeSnapshotReply();

        ImageInventory image = ImageInventory.valueOf(dbf.findByUuid(msg.getImageUuid(), ImageVO.class));

        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg cmsg = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg();
        cmsg.setImageInventory(image);
        cmsg.setVolumeSnapshot(VolumeSnapshotInventory.valueOf(currentRoot));
        cmsg.setEncrypted(msg.getEncrypted());
        cmsg.setSystemTags(msg.getSystemTags());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPrimaryStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg, completion) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    bus.reply(msg, reply);
                    completion.done();
                    return;
                }

                CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply cr = r.castReply();
                reply.setLocationHostUuid(cr.getLocateHostUuid());
                reply.setActualSize(cr.getActualSize());

                if (cr.isIncremental()) {
                    VolumeSnapshotReferenceVO ref = new VolumeSnapshotReferenceVO();
                    ref.setReferenceUuid(msg.getImageUuid());
                    ref.setReferenceType(ImageCacheVO.class.getSimpleName());
                    ref.setReferenceInstallUrl(cr.getInventory().getInstallUrl());
                    ref.setVolumeSnapshotInstallUrl(currentRoot.getPrimaryStorageInstallPath());
                    ref.setDirectSnapshotInstallUrl(currentRoot.getPrimaryStorageInstallPath());
                    ref.setDirectSnapshotUuid(currentRoot.getUuid());
                    ref.setVolumeSnapshotUuid(currentRoot.getUuid());
                    ref.setVolumeUuid(currentRoot.getVolumeUuid());
                    dbf.persist(ref);

                    logger.debug(String.format("refer image cache[uuid:%s] from volume snapshot[uuid:%s, volumeUuid:%s]" +
                            " because of incremental image cache creation", msg.getImageUuid(), currentRoot.getUuid(), currentRoot.getVolumeUuid()));

                    reply.setImageUrl(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA + currentRoot.getUuid());
                }

                bus.reply(msg, reply);
                completion.done();
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteVolumeSnapshotMsg) {
            handle((APIDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIRevertVolumeFromSnapshotMsg) {
            handle((APIRevertVolumeFromSnapshotMsg) msg);
//        } else if (msg instanceof APIBackupVolumeSnapshotMsg) {
//            handle((APIBackupVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotFromBackupStorageMsg) {
            handle((APIDeleteVolumeSnapshotFromBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateVolumeSnapshotMsg) {
            handle((APIUpdateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIGetVolumeSnapshotSizeMsg ) {
            handle((APIGetVolumeSnapshotSizeMsg) msg);
        } else if (msg instanceof APIShrinkVolumeSnapshotMsg) {
            handle((APIShrinkVolumeSnapshotMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVolumeSnapshotMsg msg) {
        APIUpdateVolumeSnapshotEvent evt = new APIUpdateVolumeSnapshotEvent(msg.getId());
        final FlowChain flowChain = FlowChainBuilder.newShareFlowChain();
        flowChain.setName(String.format("update-volume-Snapshot-%s", msg.getUuid()));
        flowChain.allowWatch();
        flowChain.then(new ShareFlow() {
            VolumeSnapshotVO self = dbf.findByUuid(msg.getUuid(), VolumeSnapshotVO.class);
            boolean update = false;
            final VolumeSnapshotVO beforeUpdateVolumeSnapshotVO = self;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "update-volume-snapshot-vo";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (msg.getName() != null) {
                            self.setName(msg.getName());
                            update = true;
                        }
                        if (msg.getDescription() != null) {
                            self.setDescription(msg.getDescription());
                            update = true;
                        }
                        if (update) {
                            dbf.update(self);
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        dbf.update(beforeUpdateVolumeSnapshotVO);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "call-pre-update-volume-snapshot-extensions";

                    @Override
                    public boolean skip(Map data) {
                        return pluginRgty.getExtensionList(AfterUpdateVolumeSnapshotExtensionPoint.class).isEmpty();
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        new While<>(pluginRgty.getExtensionList(AfterUpdateVolumeSnapshotExtensionPoint.class)).each((ext, whileCompletion) -> {
                            ext.afterUpdateVolumeSnapshot(self, msg, new Completion(trigger) {
                                @Override
                                public void success() {
                                    whileCompletion.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    whileCompletion.addError(errorCode);
                                    whileCompletion.allDone();
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
                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        refreshVO();
                        evt.setInventory(VolumeSnapshotInventory.valueOf(self));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void shrinkSnapshot(APIShrinkVolumeSnapshotMsg msg, final ReturnValueCompletion<ShrinkResult> completion) {
        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            completion.fail(err);
            return;
        }

        ShrinkVolumeSnapshotOnPrimaryStorageMsg smsg = new ShrinkVolumeSnapshotOnPrimaryStorageMsg();
        smsg.setSnapshotUuid(currentRoot.getUuid());
        smsg.setPrimaryStorageUuid(currentRoot.getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, currentRoot.getPrimaryStorageUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    completion.fail(rly.getError());
                    return;
                }

                ShrinkVolumeSnapshotOnPrimaryStorageReply reply = (ShrinkVolumeSnapshotOnPrimaryStorageReply) rly;
                ShrinkResult shrinkResult = reply.getShrinkResult();
                if (shrinkResult.getDeltaSize() != 0) {
                    currentRoot.setSize(shrinkResult.getSize());
                    dbf.updateAndRefresh(currentRoot);
                }
                completion.success(shrinkResult);
            }
        });
    }

    private void handle(APIShrinkVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIShrinkVolumeSnapshotEvent event = new APIShrinkVolumeSnapshotEvent(msg.getId());

                shrinkSnapshot(msg, new ReturnValueCompletion<ShrinkResult>(msg) {
                    @Override
                    public void success(ShrinkResult returnValue) {
                        event.setShrinkResult(returnValue);
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("shrink-snapshot-%s-for-volume-%s", currentRoot.getUuid(), currentRoot.getVolumeUuid());
            }
        });
    }

    private void handle(APIGetVolumeSnapshotSizeMsg msg) {
        APIGetVolumeSnapshotSizeEvent event = new APIGetVolumeSnapshotSizeEvent(msg.getId());

        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getUuid(), VolumeSnapshotVO.class);
        if (snapshotVO.getPrimaryStorageUuid() == null) {
            event.setActualSize(0L);
            event.setSize(0L);
            bus.publish(event);
            return;
        }

        GetVolumeSnapshotSizeOnPrimaryStorageMsg getVolumeSnapshotStatusMsg = new GetVolumeSnapshotSizeOnPrimaryStorageMsg();
        getVolumeSnapshotStatusMsg.setPrimaryStorageUuid(snapshotVO.getPrimaryStorageUuid());
        getVolumeSnapshotStatusMsg.setSnapshotUuid(snapshotVO.getUuid());
        bus.makeTargetServiceIdByResourceUuid(getVolumeSnapshotStatusMsg, PrimaryStorageConstant.SERVICE_ID, snapshotVO.getPrimaryStorageUuid());

        bus.send(getVolumeSnapshotStatusMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    event.setError(rly.getError());
                    bus.publish(event);
                    return;
                }

                GetVolumeSnapshotSizeOnPrimaryStorageReply reply = (GetVolumeSnapshotSizeOnPrimaryStorageReply) rly;

                if (reply.getActualSize() != null && reply.getActualSize() > 0) {
                    SQL.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, msg.getUuid())
                            .set(VolumeSnapshotVO_.size, reply.getActualSize())
                            .update();
                }

                event.setActualSize(reply.getActualSize());
                event.setSize(reply.getSize());
                bus.publish(event);
            }
        });
    }

    // The logic for cleaning up snapshots when deleting a snapshot chain
    private boolean cleanup() {

        List<VolumeSnapshotInventory> snapshots = currentLeaf.getDescendants();
        boolean cleanup = new SQLBatchWithReturn<Boolean>() {
            @Override
            protected Boolean scripts() {
                String psUuid = q(VolumeSnapshotVO.class)
                        .select(VolumeSnapshotVO_.primaryStorageUuid)
                        .eq(VolumeSnapshotVO_.uuid, currentRoot.getUuid())
                        .findValue();

                if (psUuid != null) {
                    return false;
                }

                // the snapshot is on neither primary storage, delete it and descendants

                List<String> uuids = snapshots.stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toList());
                if (!uuids.isEmpty()) {
                    sql(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, uuids).hardDelete();
                }

                if (!q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, currentRoot.getTreeUuid()).isExists()) {
                    logger.debug(String.format("volume snapshot tree[uuid:%s] has no leaf, delete it", currentRoot.getTreeUuid()));
                    sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, currentRoot.getTreeUuid()).hardDelete();
                }

                VolumeSnapshotReferenceUtils.handleSnapshotDeletion(currentRoot);

                return true;
            }
        }.execute();

        if (cleanup) {
            ungroupAfterDeleted(snapshots);
        }

        return cleanup;
    }

    // The logic for cleaning up snapshot groups when deleting a snapshot chain
    private void ungroupAfterDeleted(List<VolumeSnapshotInventory> snapshots) {
        List<String> uuids = snapshots.stream().map(VolumeSnapshotInventory::getUuid).collect(Collectors.toList());
        SQL.New(VolumeSnapshotGroupRefVO.class).in(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, uuids)
                .set(VolumeSnapshotGroupRefVO_.snapshotDeleted, true).update();
        if (currentRoot.getVolumeType().equals(VolumeType.Root.toString())) {
            List<String> groupUuids = new ArrayList<>();
            for (VolumeSnapshotInventory snapshot : snapshots) {
                String groupUuid = snapshot.getGroupUuid();
                if (groupUuid != null) {
                    logger.debug(String.format("root volume snapshot[uuid:%s, name:%s] has been deleted, " +
                            "ungroup snapshot group[uuid:%s]", snapshot.getUuid(), snapshot.getName(), groupUuid));
                    groupUuids.add(groupUuid);
                }

            }

            groupUuids.forEach(groupUuid -> vidm.deleteArchiveVmInstanceResourceMetadataGroup(groupUuid));
            cleanVmHostBackupFilesForGroup(groupUuids);
            dbf.removeByPrimaryKeys(groupUuids, VolumeSnapshotGroupVO.class);
        }
    }

    private void cleanVmHostBackupFilesForGroup(List<String> groupUuids) {
        // TODO: refactor this: VolumeSnapshotGroupVO should has its own cascade extensions!
        //       VolumeSnapshotGroupVO -> VmHostBackupFileVO
        if (groupUuids.isEmpty()) {
            return;
        }

        List<String> backupUuidList = Q.New(VmHostBackupFileVO.class)
                .in(VmHostBackupFileVO_.resourceUuid, groupUuids)
                .select(VmHostBackupFileVO_.uuid)
                .listValues();
        if (!backupUuidList.isEmpty()) {
            new While<>(backupUuidList).each((fileUuid, whileCompletion) -> {
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
            }).run(new WhileDoneCompletion(null) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (errorCodeList.hasError()) {
                        logger.warn("failed to delete some VmHostBackupFiles:\n" + String.join("\n",
                                transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails)));
                    }
                }
            });
        }
    }

    private List<VolumeSnapshotBackupStorageDeletionMsg> makeVolumeSnapshotBackupStorageDeletionMsg(List<String> bsUuids) {
        List<VolumeSnapshotBackupStorageDeletionMsg> msgs = new ArrayList<>();

        List<String> allMyBsUuids = transformAndRemoveNull(currentRoot.getBackupStorageRefs(), VolumeSnapshotBackupStorageRefVO::getBackupStorageUuid);

        if (allMyBsUuids.isEmpty()) {
            return msgs;
        }

        if (bsUuids == null || bsUuids.containsAll(allMyBsUuids)) {
            // delete me and all my descendants
            for (VolumeSnapshotInventory inv : currentLeaf.getDescendants()) {
                List<String> buuids = transformAndRemoveNull(inv.getBackupStorageRefs(), VolumeSnapshotBackupStorageRefInventory::getBackupStorageUuid);
                VolumeSnapshotBackupStorageDeletionMsg msg = new VolumeSnapshotBackupStorageDeletionMsg();
                msg.setBackupStorageUuids(buuids);
                msg.setSnapshotUuid(inv.getUuid());
                bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
                msgs.add(msg);
            }
        } else {
            // delete me only
            VolumeSnapshotBackupStorageDeletionMsg msg = new VolumeSnapshotBackupStorageDeletionMsg();
            msg.setSnapshotUuid(currentRoot.getUuid());
            msg.setBackupStorageUuids(bsUuids);
            bus.makeLocalServiceId(msg, VolumeSnapshotConstant.SERVICE_ID);
            msgs.add(msg);
        }

        return msgs;
    }

    private void handle(final APIDeleteVolumeSnapshotFromBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deleteOnBackupStorage(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-volume-snapshot-%s-on-backup-storage", currentRoot.getUuid());
            }
        });
    }

    private void deleteOnBackupStorage(final APIDeleteVolumeSnapshotFromBackupStorageMsg msg, final NoErrorCompletion completion) {
        final APIDeleteVolumeSnapshotFromBackupStorageEvent evt = new APIDeleteVolumeSnapshotFromBackupStorageEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setError(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-from-backup-stroage", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volume-snapshot-from-backup-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<VolumeSnapshotBackupStorageDeletionMsg> msgs = makeVolumeSnapshotBackupStorageDeletionMsg(msg.getBackupStorageUuids());
                        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                //delete would always succeed
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        if (!cleanup()) {
                            currentRoot = dbf.reload(currentRoot);
                            evt.setInventory(getSelfInventory());
                        }

                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }

/*
    private void handle(final APIBackupVolumeSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                backup(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("backup-volume-snapshot-%s", msg.getUuid());
            }
        });
    }

    private void backup(final APIBackupVolumeSnapshotMsg msg, final NoErrorCompletion completion) {
        final APIBackupVolumeSnapshotEvent evt = new APIBackupVolumeSnapshotEvent(msg.getId());

        refreshVO();
        final ErrorCode err = isOperationAllowed(msg);
        if (err != null) {
            evt.setError(err);
            bus.publish(evt);
            completion.done();
            return;
        }

        class Info {
            VolumeSnapshotInventory snapshot;
            String backupStorageUuid;
            BackupStorageInventory destBackupStorage;
            boolean backupSuccess;
        }

        final String requiredBsUuid = msg.getBackupStorageUuid();
        final List<Info> needBackup = new ArrayList<Info>();
        currentLeaf.walkUp(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                Info info = new Info();
                info.snapshot = arg;

                if (arg.getUuid().equals(currentRoot.getUuid()) && requiredBsUuid != null) {
                    info.backupStorageUuid = requiredBsUuid;
                    needBackup.add(info);
                    return false;
                }

                if (arg.getBackupStorageRefs().isEmpty()) {
                    needBackup.add(info);
                }

                return false;
            }
        });

        if (needBackup.isEmpty()) {
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("backup-volume-snapshot-%s", currentRoot.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-backup-storage";

                    private void allocateBackupStorage(final Iterator<Info> it, final FlowTrigger trigger) {
                        if (!it.hasNext()) {
                            trigger.next();
                            return;
                        }

                        final Info info = it.next();
                        AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                        amsg.setBackupStorageUuid(info.backupStorageUuid);
                        amsg.setSize(info.snapshot.getSize());
                        bus.makeLocalServiceId(amsg, BackupStorageConstant.SERVICE_ID);
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    AllocateBackupStorageReply re = reply.castReply();
                                    info.destBackupStorage = re.getInventory();
                                    allocateBackupStorage(it, trigger);
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        allocateBackupStorage(needBackup.iterator(), trigger);
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        for (Info info : needBackup) {
                            if (info.destBackupStorage != null) {
                                ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                                rmsg.setBackupStorageUuid(info.destBackupStorage.getUuid());
                                rmsg.setSize(info.snapshot.getSize());
                                bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, info.destBackupStorage.getUuid());
                                bus.send(rmsg);
                            }
                        }

                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "backing-up";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        List<BackupVolumeSnapshotMsg> bmsgs = CollectionUtils.transformToList(needBackup, new Function<BackupVolumeSnapshotMsg, Info>() {
                            @Override
                            public BackupVolumeSnapshotMsg call(Info arg) {
                                BackupVolumeSnapshotMsg bmsg = new BackupVolumeSnapshotMsg();
                                bmsg.setBackupStorage(arg.destBackupStorage);
                                bmsg.setSnapshotUuid(arg.snapshot.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(bmsg, VolumeSnapshotConstant.SERVICE_ID, arg.snapshot.getUuid());
                                return bmsg;
                            }
                        });

                        bus.send(bmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_BACKUP_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                ErrorCode err = null;
                                for (MessageReply r : replies) {
                                    if (r.isSuccess()) {
                                        Info info = needBackup.get(replies.indexOf(r));
                                        info.backupSuccess = true;
                                        logger.debug(String.format("successfully backed up volume snapshot[uuid:%s] on backup storage[uuid:%s]",
                                                info.snapshot.getUuid(), info.destBackupStorage.getUuid()));
                                    } else {
                                        err = r.getError();
                                    }
                                }

                                if (err != null) {
                                    trigger.fail(err);
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        List<VolumeSnapshotBackupStorageDeletionMsg> dmsgs = CollectionUtils.transformToList(needBackup, new Function<VolumeSnapshotBackupStorageDeletionMsg, Info>() {
                            @Override
                            public VolumeSnapshotBackupStorageDeletionMsg call(Info arg) {
                                if (arg.backupSuccess) {
                                    VolumeSnapshotBackupStorageDeletionMsg dmsg = new VolumeSnapshotBackupStorageDeletionMsg();
                                    dmsg.setSnapshotUuid(arg.snapshot.getUuid());
                                    dmsg.setBackupStorageUuids(Arrays.asList(arg.destBackupStorage.getUuid()));
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, arg.snapshot.getUuid());
                                    return dmsg;
                                }
                                return null;
                            }
                        });

                        if (dmsgs.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        bus.send(dmsgs, VolumeSnapshotGlobalConfig.SNAPSHOT_BACKUP_PARALLELISM_DEGREE.value(Integer.class), new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        refreshVO();
                        evt.setInventory(getSelfInventory());
                        bus.publish(evt);
                        completion.done();
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                        completion.done();
                    }
                });
            }
        }).start();
    }
*/

    private void handle(final RevertVolumeFromSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(SyncTaskChain chain) {
                RevertVolumeFromSnapshotGroupReply reply = new RevertVolumeFromSnapshotGroupReply();
                revert(msg, new Completion(msg, chain) {
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
                return String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid());
            }
        });
    }

    private void handle(final APIRevertVolumeFromSnapshotMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return syncSignature;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIRevertVolumeFromSnapshotEvent evt = new APIRevertVolumeFromSnapshotEvent(msg.getId());

                revert(msg, new Completion(evt, chain) {
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
                return String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid());
            }
        });
    }

    private void revert(final RevertVolumeSnapshotMessage msg, final Completion completion) {
        refreshVO();
        final ErrorCode err = isOperationAllowed((Message) msg);
        if (err != null) {
            completion.fail(err);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("revert-volume-%s-from-snapshot-%s", currentRoot.getVolumeUuid(), currentRoot.getUuid()));
        chain.enableProgressReport();
        chain.then(new ShareFlow() {
            String newVolumeInstallPath;
            Long trashId;
            long newSize;
            long actualSize;
            VolumeVO volume = dbf.findByUuid(currentRoot.getVolumeUuid(), VolumeVO.class);
            VolumeInventory volumeInventory = VolumeInventory.valueOf(volume);
            String oldVolumeInstallPath = volume.getInstallPath();
            VolumeSnapshotInventory newSnapshot;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-check";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (String vmUuid : volumeInventory.getAttachedVmUuids()) {
                            VmInstanceState state = Q.New(VmInstanceVO.class)
                                    .select(VmInstanceVO_.state)
                                    .eq(VmInstanceVO_.uuid, vmUuid)
                                    .findValue();
                            if (state != VmInstanceState.Stopped) {
                                trigger.fail(operr("unable to reset volume[uuid:%s] to snapshot[uuid:%s]," +
                                                " the vm[uuid:%s] volume attached to is not in Stopped state," +
                                                " current state is %s",
                                        volumeInventory.getUuid(),
                                        currentRoot.getUuid(),
                                        vmUuid, state));
                                return;
                            }
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-snapshot-for-current-volume-first";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!VolumeSnapshotGlobalConfig.SNAPSHOT_BEFORE_REVERTVOLUME.value(Boolean.class)) {
                            logger.debug("skip create snapshot because of global config");
                            trigger.next();
                            return;
                        }
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName(String.format("revert-volume-point-%s-%s", volume.getUuid(), TimeUtils.getCurrentTimeStamp("yyyyMMddHHmmss")));
                        cmsg.setDescription(String.format("save snapshot for revert volume [uuid:%s]", volume.getUuid()));
                        cmsg.setVolumeUuid(volume.getUuid());
                        cmsg.setAccountUuid(msg.getSession().getAccountUuid());

                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                } else {
                                    newSnapshot = ((CreateVolumeSnapshotReply) r).getInventory();
                                    new SnapshotCanonicalEvents
                                            .InnerVolumeSnapshotCreated(volume.getUuid(), volume.getPrimaryStorageUuid(), newSnapshot)
                                            .fire();
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                if (msg instanceof RevertVolumeFromSnapshotGroupMsg) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "add-snapshot-to-group";
                        String newGroupUuid = ((RevertVolumeFromSnapshotGroupMsg) msg).getNewSnapshotGroupUuid();

                        @Override
                        public boolean skip(Map data) {
                            return newSnapshot == null || newGroupUuid == null;
                        }

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            VolumeSnapshotGroupRefVO ref = new VolumeSnapshotGroupRefVO();
                            ref.setVolumeSnapshotGroupUuid(newGroupUuid);
                            ref.setVolumeSnapshotUuid(newSnapshot.getUuid());
                            ref.setVolumeSnapshotInstallPath(newSnapshot.getPrimaryStorageInstallPath());
                            ref.setVolumeSnapshotName(newSnapshot.getName());
                            ref.setVolumeUuid(volumeInventory.getUuid());
                            ref.setVolumeName(volumeInventory.getName());
                            ref.setVolumeType(volumeInventory.getType());
                            ref.setDeviceId(volumeInventory.getDeviceId());
                            ref.setVolumeLastAttachDate(volumeInventory.getLastAttachDate());
                            dbf.persist(ref);
                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "revert-volume-before-check-snap-integrity";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<BeforeRevertVolumeFromSnapshotExtensionPoint> extensionList = pluginRgty.getExtensionList(BeforeRevertVolumeFromSnapshotExtensionPoint.class);
                        if (extensionList.isEmpty()) {
                            trigger.next();
                        }

                        extensionList.forEach(exp -> exp.beforeRevertVolumeFromSnapshot(getSelfInventory(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }));
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "revert-volume-from-volume-snapshot-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        RevertVolumeFromSnapshotOnPrimaryStorageMsg rmsg = new RevertVolumeFromSnapshotOnPrimaryStorageMsg();
                        rmsg.setSnapshot(getSelfInventory());
                        rmsg.setVolume(volumeInventory);

                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, volumeInventory.getPrimaryStorageUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    RevertVolumeFromSnapshotOnPrimaryStorageReply re = (RevertVolumeFromSnapshotOnPrimaryStorageReply) reply;
                                    newVolumeInstallPath = re.getNewVolumeInstallPath();
                                    newSize = re.getSize();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-volume-old-install-path-size";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        if (!oldVolumeInstallPath.equals(newVolumeInstallPath)) {
                            SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();
                            smsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                            smsg.setVolumeUuid(volume.getUuid());
                            smsg.setInstallPath(oldVolumeInstallPath);
                            bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                            bus.send(smsg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to get volume[%s] old install path[%s] actualSize, because:%s", volume.getUuid(), oldVolumeInstallPath, reply.getError()));
                                    } else {
                                        SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                                        actualSize = r.getActualSize();
                                        if (actualSize > 0) {
                                            PrimaryStorageCapacityUpdater updater =
                                                    new PrimaryStorageCapacityUpdater(volume.getPrimaryStorageUuid());
                                            updater.reserve(actualSize);
                                        }
                                    }
                                    trigger.next();
                                }
                            });
                        } else {
                            logger.info(String.format("skip get volume old install path size, because volume installPath has not changed"));
                            trigger.next();
                        }
                    }
                });

                flow(new Flow() {
                    String __name__ = "move old install path to trash if no snapshots";

                    @Override
                    public boolean skip(Map data) {
                        logger.debug(String.format("volume[uuid:%s] has been used new install path[%s], " +
                                "check old install path[%s] is still in used or not.",
                                volume.getUuid(), newVolumeInstallPath, oldVolumeInstallPath));
                        return oldVolumeInstallPath.equals(newVolumeInstallPath);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!VolumeSnapshotGlobalConfig.SNAPSHOT_BEFORE_REVERTVOLUME.value(Boolean.class)) {
                            VolumeInventory vol = VolumeInventory.valueOf(volume);
                            vol.setSize(actualSize);
                            vol.setInstallPath(oldVolumeInstallPath);
                            vol.setPrimaryStorageUuid(getSelfInventory().getPrimaryStorageUuid());
                            trashId = trash.createTrash(TrashType.RevertVolume, false, vol).getTrashId();
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (trashId != null) {
                            trash.removeFromDb(trashId);
                        }
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Transactional
                    private void updateLatest() {
                        String sql = "update VolumeSnapshotVO s" +
                                " set s.latest = false" +
                                " where s.latest = true" +
                                " and s.treeUuid = :treeUuid";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("treeUuid", currentRoot.getTreeUuid());
                        q.executeUpdate();

                        currentRoot.setLatest(true);
                        dbf.getEntityManager().merge(currentRoot);

                        sql = "update VolumeSnapshotTreeVO tree" +
                                " set tree.current = false" +
                                " where tree.current = true" +
                                " and tree.volumeUuid = :volUuid";
                        q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("volUuid", currentRoot.getVolumeUuid());
                        q.executeUpdate();

                        sql = "update VolumeSnapshotTreeVO tree" +
                                " set tree.current = true" +
                                " where tree.uuid = :treeUuid";
                        q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("treeUuid", currentRoot.getTreeUuid());
                        q.executeUpdate();
                    }

                    private void updateCapacity(long increment) {
                        if (increment > 0) {
                            DecreasePrimaryStorageCapacityMsg dmsg = new DecreasePrimaryStorageCapacityMsg();
                            dmsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                            dmsg.setDiskSize(increment);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                            bus.send(dmsg);
                        } else if (increment < 0) {
                            IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                            imsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
                            imsg.setDiskSize(Math.abs(increment));
                            bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
                            bus.send(imsg);
                        }
                    }

                    @Override
                    public void handle(Map data) {
                        String oldVolumeInstallPath = volume.getInstallPath();
                        Integer oldKeyVersion = volume.isEncrypted() && !currentRoot.isEncrypted() ?
                                volumeEncryptedResourceKeyBackend.findKeyVersionByVolume(volume.getUuid()) : null;
                        String oldSecretHostUuid = volume.isEncrypted() && !currentRoot.isEncrypted() ?
                                resolveVolumeSecretHostUuid(volume) : null;
                        String oldSecretVmUuid = StringUtils.defaultIfBlank(volume.getVmInstanceUuid(), volume.getUuid());

                        volume.setInstallPath(newVolumeInstallPath);
                        volume.setEncrypted(currentRoot.isEncrypted());

                        // if resized capacity should be updated
                        if (newSize != 0) {
                            updateCapacity(newSize - volume.getSize());
                            volume.setSize(newSize);
                        }

                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                merge(volume);
                                syncVolumeEncryptionAfterRevert(volume.getUuid(), currentRoot);
                                updateLatest();
                            }
                        }.execute();

                        if (oldKeyVersion != null) {
                            volumeEncryptedSecretHelper.deleteSecretOnHostBestEffort(
                                    oldSecretHostUuid, oldSecretVmUuid, volume.getUuid(), oldKeyVersion);
                        }
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.warn(String.format("failed to restore volume[uuid:%s] to snapshot[uuid:%s, name:%s], %s",
                                volumeInventory.getUuid(), currentRoot.getUuid(), currentRoot.getName(), errCode));
                        completion.fail(errCode);
                    }
                });
            }
        }).start();

    }

    private void syncVolumeEncryptionAfterRevert(String volumeUuid, VolumeSnapshotVO snapshot) {
        if (snapshot.isEncrypted()) {
            volumeEncryptedResourceKeyBackend.copySnapshotKeyRefToVolume(snapshot.getUuid(), volumeUuid);
        } else {
            volumeEncryptedResourceKeyBackend.detachKeyProviderFromVolume(volumeUuid);
        }
    }

    private String resolveVolumeSecretHostUuid(VolumeVO volume) {
        if (volume == null) {
            return null;
        }

        List<String> tags = VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTags(volume.getUuid(), VolumeVO.class);
        if (tags != null && !tags.isEmpty()) {
            return VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.getTokenByTag(
                    tags.get(0), VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST_TOKEN);
        }

        String vmUuid = volume.getVmInstanceUuid();
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }

        String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (StringUtils.isNotBlank(hostUuid)) {
            return hostUuid;
        }

        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.lastHostUuid)
                .findValue();
    }

    private void deleteVolumeSnapshot(final DeleteVolumeSnapshotMessage msg, Completion completion) {
        final String issuer = VolumeSnapshotVO.class.getSimpleName();
        VolumeSnapshotDeletionStructs ctx = new VolumeSnapshotDeletionStructs(
                Collections.singletonList(getSelfInventory()), msg.getDirection(), msg.getScope());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-snapshot-%s", msg.getSnapshotUuid()));
        chain.enableProgressReport();

        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
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
                new FireSnapShotCanonicalEvent()
                        .fireSnapShotStatusChangedEvent(currentRoot.getStatus(), VolumeSnapshotInventory.valueOf(currentRoot));
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIDeleteVolumeSnapshotMsg msg) {
        final APIDeleteVolumeSnapshotEvent evt = new APIDeleteVolumeSnapshotEvent(msg.getId());

        deleteVolumeSnapshot(msg, new Completion(msg) {
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

    private void markSnapshotAsVolume(VolumeInventory volume,long volumeInstallPathSize, String newInstallPath, long newInstallPathSize) {
                refreshVO();
                if (!currentRoot.isLatest()) {
                    throw new RuntimeException(String.format("current snapshot:%s is not latest snapshot, cannot mark as volume", currentRoot.getUuid()));
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        sql(VolumeVO.class).eq(VolumeVO_.uuid, volume.getUuid())
                                .set(VolumeVO_.installPath, newInstallPath)
                                .set(VolumeVO_.actualSize, newInstallPathSize)
                                .update();

                        sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, currentRoot.getUuid())
                                .set(VolumeSnapshotVO_.primaryStorageInstallPath, volume.getInstallPath())
                                .set(VolumeSnapshotVO_.size, volumeInstallPathSize)
                                .set(VolumeSnapshotVO_.latest, false)
                                .update();

                        if (currentRoot.getParentUuid() != null) {
                            // reset latest
                            sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, currentRoot.getParentUuid())
                                    .set(VolumeSnapshotVO_.latest, true).update();
                            logger.debug(String.format("reset latest snapshot of tree[uuid:%s] to snapshot[uuid:%s]",
                                    currentRoot.getTreeUuid(), currentRoot.getParentUuid()));
                        }

                        if (currentRoot.getParentUuid() == null) {
                            sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, currentRoot.getTreeUuid())
                                    .set(VolumeSnapshotTreeVO_.current, false).update();
                        }

                        VolumeSnapshotReferenceUtils.updateReferenceAfterMarkSnapshotAsVolume(currentRoot);
                    }
                }.execute();
    }
}
