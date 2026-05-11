package org.zstack.storage.snapshot.group;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.tpm.message.AddTpmMsg;
import org.zstack.header.tpm.message.DeleteTpmKeyBackupMsg;
import org.zstack.header.tpm.message.RestoreTpmEncryptionKeyMsg;
import org.zstack.header.tpm.message.RestoreTpmEncryptionKeyReply;
import org.zstack.header.tpm.message.TpmDeletionMsg;
import org.zstack.header.vm.additions.RestoreVmHostFileMsg;
import org.zstack.header.vm.RestoreVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.additions.VmHostFileManager;
import org.zstack.header.vm.additions.VmHostFileSyncReason;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.storage.snapshot.VolumeSnapshotGroupSystemTags;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.tpm.TpmConstants.SERVICE_ID;
import static org.zstack.storage.snapshot.VolumeSnapshotMessageRouter.getResourceIdToRouteMsg;

/**
 * Created by MaJin on 2019/7/9.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotGroupBase implements VolumeSnapshotGroup {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotGroupBase.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private VmHostFileManager vmHostFileManager;

    public VolumeSnapshotGroupBase(VolumeSnapshotGroupVO self) {
        this.self = self;
        this.id = "volumeSnapshotGroup-" + self.getUuid();
    }

    protected VolumeSnapshotGroupVO self;

    protected final String id;

    protected VolumeSnapshotGroupInventory getSelfInventory() {
        return VolumeSnapshotGroupInventory.valueOf(self);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteVolumeSnapshotGroupInnerMsg) {
            handle((DeleteVolumeSnapshotGroupInnerMsg) msg);
        } else if (msg instanceof RevertVmFromSnapshotGroupInnerMsg) {
            handle((RevertVmFromSnapshotGroupInnerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUngroupVolumeSnapshotGroupMsg) {
            handle((APIUngroupVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotGroupMsg) {
            handle((APIDeleteVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIRevertVmFromSnapshotGroupMsg) {
            handle((APIRevertVmFromSnapshotGroupMsg) msg);
        } else if (msg instanceof APIUpdateVolumeSnapshotGroupMsg) {
            handle((APIUpdateVolumeSnapshotGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVolumeSnapshotGroupMsg msg) {
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
            dbf.updateAndRefresh(self);
        }

        APIUpdateVolumeSnapshotGroupEvent event = new APIUpdateVolumeSnapshotGroupEvent(msg.getId());
        event.setInventory(getSelfInventory());
        bus.publish(event);
    }

    private void handle(APIUngroupVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUngroupVolumeSnapshotGroupEvent evt = new APIUngroupVolumeSnapshotGroupEvent(msg.getId());
                dbf.remove(self);
                vmHostFileManager.cleanVmHostBackupFile(msg.getUuid());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return "ungroup-snapshot-group";
            }
        });
    }

    private void handle(APIDeleteVolumeSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleDelete(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-snapshot-group";
            }
        });
    }

    private void handleDelete(APIDeleteVolumeSnapshotGroupMsg msg, NoErrorCompletion completion) {
        APIDeleteVolumeSnapshotGroupEvent event = new APIDeleteVolumeSnapshotGroupEvent(msg.getId());
        DeleteVolumeSnapshotGroupInnerMsg imsg = new DeleteVolumeSnapshotGroupInnerMsg();
        imsg.setUuid(msg.getUuid());
        imsg.setDeletionMode(msg.getDeletionMode());
        imsg.setScope(msg.getScope());
        imsg.setDirection(msg.getDirection());
        bus.makeTargetServiceIdByResourceUuid(imsg, VolumeSnapshotConstant.SERVICE_ID, msg.getUuid());
        overlaySend(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }

                if (reply instanceof DeleteVolumeSnapshotGroupInnerReply) {
                    event.setResults(((DeleteVolumeSnapshotGroupInnerReply) reply).getResults());
                }

                bus.publish(event);
                completion.done();
            }
        });
    }

    private void handle(DeleteVolumeSnapshotGroupInnerMsg msg) {
        DeleteVolumeSnapshotGroupInnerReply reply = new DeleteVolumeSnapshotGroupInnerReply();
        List<VolumeSnapshotVO> snapshots = getEffectiveSnapshots();
        if (snapshots.size() < self.getSnapshotCount()) {
            logger.debug(String.format("skip snapshots not belong to origin vm[uuid:%s]", self.getVmInstanceUuid()));
        }

        SimpleFlowChain.of("delete-volume-snapshot-group")
            .then("delete-volume-snapshots", (trigger) ->
                new While<>(snapshots).step((snapshot, compl) -> {
                    DeleteVolumeSnapshotMsg rmsg = new DeleteVolumeSnapshotMsg();
                    rmsg.setSnapshotUuid(snapshot.getUuid());
                    rmsg.setVolumeUuid(snapshot.getVolumeUuid());
                    rmsg.setTreeUuid(snapshot.getTreeUuid());
                    rmsg.setDeletionMode(msg.getDeletionMode());
                    rmsg.setScope(msg.getScope());
                    rmsg.setDirection(msg.getDirection());
                    bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeSnapshotConstant.SERVICE_ID, getResourceIdToRouteMsg(snapshot));
                    bus.send(rmsg, new CloudBusCallBack(compl) {
                        @Override
                        public void run(MessageReply r) {
                            reply.addResult(new DeleteSnapshotGroupResult(rmsg.getSnapshotUuid(), rmsg.getVolumeUuid(), r.getError()));
                            compl.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                }))
            .then("delete-vm-host-backup-files", trigger -> {
                vmHostFileManager.cleanVmHostBackupFile(self.getUuid());
                trigger.next();
            })
            .propagateExceptionTo(msg)
            .done(() -> bus.reply(msg, reply))
            .error(errorCode -> {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            })
            .start();
    }

    private void handle(APIRevertVmFromSnapshotGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return id;
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleRevert(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "revert-snapshot-group";
            }
        });
    }

    private void handleRevert(APIRevertVmFromSnapshotGroupMsg msg, NoErrorCompletion completion) {
        APIRevertVmFromSnapshotGroupEvent event = new APIRevertVmFromSnapshotGroupEvent(msg.getId());
        VolumeSnapshotGroupOperationValidator.validate(self.getVmInstanceUuid(),
                VolumeSnapshotGroupOperationValidator.Operation.REVERT);

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("revert-vm-%s-from-snapshot-group-%s", self.getVmInstanceUuid(), msg.getGroupUuid()));
        chain.getData().put(VolumeSnapshotGroupConstant.Parmas.SnapshotGroupUuid.toString(), self.getUuid());
        chain.getData().put(VolumeSnapshotGroupConstant.Parmas.SnapshotGroup.toString(), getSelfInventory());
        pluginRgty.getExtensionList(RevertVmFromSnapShotGroupExtension.class)
                .stream()
                .filter(RevertVmFromSnapShotGroupExtension::needRunExtension)
                .forEach(v -> chain.then(v.getBeforeRevertFlow()));

        chain.then(new NoRollbackFlow() {
            String __name__ = "revert-volume-snapshots";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                RevertVmFromSnapshotGroupInnerMsg imsg = new RevertVmFromSnapshotGroupInnerMsg();
                imsg.setUuid(msg.getUuid());
                imsg.setSession(msg.getSession());
                bus.makeTargetServiceIdByResourceUuid(imsg, VolumeSnapshotConstant.SERVICE_ID, msg.getUuid());
                overlaySend(imsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        if (reply instanceof RevertVmFromSnapshotGroupInnerReply) {
                            event.setResults(((RevertVmFromSnapshotGroupInnerReply) reply).getResults());
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, self.getVmInstanceUuid()).eq(VolumeVO_.type, VolumeType.Memory).find();
                if (volume == null) {
                    trigger.next();
                    return;
                }

                Optional opt = self.getVolumeSnapshotRefs().stream().filter(sp -> sp.getVolumeUuid().equals(volume.getUuid())).findFirst();
                if (!opt.isPresent()) {
                    trigger.next();
                    return;
                }

                VolumeSnapshotGroupRefVO ref = (VolumeSnapshotGroupRefVO) opt.get();

                RestoreVmInstanceMsg rmsg = new RestoreVmInstanceMsg();
                rmsg.setVmInstanceUuid(self.getVmInstanceUuid());
                rmsg.setMemorySnapshotUuid(ref.getVolumeSnapshotUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, rmsg.getVmInstanceUuid());
                bus.send(rmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        trigger.next();
                    }
                });

            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
                completion.done();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                bus.publish(event);
                completion.done();
            }
        }).start();
    }

    private void handle(RevertVmFromSnapshotGroupInnerMsg msg) {
        RevertVmFromSnapshotGroupInnerReply reply = new RevertVmFromSnapshotGroupInnerReply();
        List<VolumeSnapshotVO> snapshots = getEffectiveSnapshots();
        String vmUuid = self.getVmInstanceUuid();

        VolumeSnapshotGroupAvailability availability = VolumeSnapshotGroupChecker.getAvailability(self);
        if (!availability.isAvailable()) {
            reply.setError(operr(availability.getReason()));
            bus.reply(msg, reply);
            return;
        }

        class Context {
            VolumeSnapshotGroupVO newGroup;
            boolean snapshotGroupHasTpm;
            String tpmUuid, newCreateTpmUuid;
            String tpmKeyBackupUuid;
        }
        Context context = new Context();
        context.snapshotGroupHasTpm = VolumeSnapshotGroupSystemTags.WITH_TPM.hasTag(msg.getUuid());
        context.tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .select(TpmVO_.uuid)
                .findValue();

        SimpleFlowChain.of("revert-vm-from-snapshot-group-inner")
            .then(Flow.of("persist-before-revert-snapshot-in-db")
                .runIf(data -> VolumeSnapshotGlobalConfig.SNAPSHOT_BEFORE_REVERTVOLUME.value(Boolean.class))
                .handle(trigger -> {
                    context.newGroup = new VolumeSnapshotGroupVO();
                    context.newGroup.setUuid(Platform.getUuid());
                    context.newGroup.setName(String.format("revert-vm-point-%s-%s", vmUuid, TimeUtils.getCurrentTimeStamp("yyyyMMddHHmmss")));
                    context.newGroup.setDescription(String.format("save snapshot for revert vm [uuid:%s]", vmUuid));
                    context.newGroup.setSnapshotCount(snapshots.size());
                    context.newGroup.setVmInstanceUuid(vmUuid);
                    context.newGroup.setAccountUuid(msg.getSession().getAccountUuid());
                    dbf.persist(context.newGroup);
                    trigger.next();
                })
                .build())
            .then(Flow.of("create-tpm-if-needed")
                .runIf(data -> context.snapshotGroupHasTpm && context.tpmUuid == null)
                .handle(trigger -> {
                    AddTpmMsg addTpmMsg = new AddTpmMsg();
                    addTpmMsg.setResourceUuidKeyFrom(msg.getUuid());
                    addTpmMsg.setVmInstanceUuid(vmUuid);
                    addTpmMsg.setTpmUuid(context.newCreateTpmUuid = Platform.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(addTpmMsg, SERVICE_ID, addTpmMsg.getTpmUuid());
                    bus.send(addTpmMsg, new CloudBusCallBack(msg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("create Tpm[uuid:%s] for VM[uuid:%s]",
                                addTpmMsg.getTpmUuid(), addTpmMsg.getVmInstanceUuid()));
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                })
                .rollback(trigger -> {
                    TpmDeletionMsg deletionMsg = new TpmDeletionMsg();
                    deletionMsg.setTpmUuid(context.newCreateTpmUuid);
                    deletionMsg.setVmInstanceUuid(vmUuid);
                    deletionMsg.setForceDelete(true);
                    bus.makeTargetServiceIdByResourceUuid(deletionMsg, SERVICE_ID, deletionMsg.getTpmUuid());
                    bus.send(deletionMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.debug(String.format("failed to deleted Tpm[uuid:%s] from VM[uuid:%s] but still continue",
                                        deletionMsg.getTpmUuid(), deletionMsg.getVmInstanceUuid()));
                            }
                            trigger.rollback();
                        }
                    });
                })
                .build())
            .then(Flow.of("restore-tpm")
                .runIf(data -> context.snapshotGroupHasTpm && context.tpmUuid != null)
                .handle(trigger -> {
                    RestoreTpmEncryptionKeyMsg restoreMsg = new RestoreTpmEncryptionKeyMsg();
                    restoreMsg.setSrcResourceUuid(msg.getUuid());
                    restoreMsg.setDstResourceUuid(context.tpmUuid);
                    restoreMsg.setBackupCurrentKey(true);
                    bus.makeTargetServiceIdByResourceUuid(restoreMsg, SERVICE_ID, context.tpmUuid);
                    bus.send(restoreMsg, new CloudBusCallBack(msg) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                if (reply instanceof RestoreTpmEncryptionKeyReply) {
                                    context.tpmKeyBackupUuid = ((RestoreTpmEncryptionKeyReply) reply).getTpmKeyBackupUuid();
                                }
                                logger.debug(String.format(
                                        "restore resource key of Tpm[uuid:%s] for VM[uuid:%s] for snapshotGroup[uuid:%s]",
                                        context.tpmUuid, vmUuid, msg.getUuid()));
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                })
                .rollback(trigger -> {
                    if (context.tpmKeyBackupUuid == null) {
                        trigger.rollback();
                        return;
                    }
                    RestoreTpmEncryptionKeyMsg rollbackMsg = new RestoreTpmEncryptionKeyMsg();
                    rollbackMsg.setBackupCurrentKey(false);
                    rollbackMsg.setSrcResourceUuid(context.tpmKeyBackupUuid);
                    rollbackMsg.setDstResourceUuid(context.tpmUuid);
                    bus.makeTargetServiceIdByResourceUuid(rollbackMsg, SERVICE_ID, context.tpmUuid);
                    bus.send(rollbackMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.debug(String.format(
                                        "failed to rollback TPM encryption key from TpmKeyBackupVO[uuid:%s] to Tpm[uuid:%s]",
                                        context.tpmKeyBackupUuid, context.tpmUuid));
                            }
                            DeleteTpmKeyBackupMsg delMsg = new DeleteTpmKeyBackupMsg();
                            delMsg.setTpmUuid(context.tpmUuid);
                            delMsg.setTpmKeyBackupUuid(context.tpmKeyBackupUuid);
                            bus.makeTargetServiceIdByResourceUuid(delMsg, SERVICE_ID, context.tpmUuid);
                            bus.send(delMsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply2) {
                                    if (!reply2.isSuccess()) {
                                        logger.debug(String.format(
                                                "failed to delete TpmKeyBackupVO[uuid:%s] after TPM key rollback",
                                                context.tpmKeyBackupUuid));
                                    }
                                    context.tpmKeyBackupUuid = null;
                                    trigger.rollback();
                                }
                            });
                        }
                    });
                })
                .build())
            .then(Flow.of("remove-tpm-if-needed")
                .runIf(data -> !context.snapshotGroupHasTpm && context.tpmUuid != null)
                .handle(trigger -> {
                    TpmDeletionMsg deletionMsg = new TpmDeletionMsg();
                    deletionMsg.setTpmUuid(context.tpmUuid);
                    deletionMsg.setVmInstanceUuid(vmUuid);
                    deletionMsg.setForceDelete(true);
                    bus.makeTargetServiceIdByResourceUuid(deletionMsg, SERVICE_ID, deletionMsg.getTpmUuid());
                    bus.send(deletionMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("deleted Tpm[uuid:%s] from VM[uuid:%s]",
                                        deletionMsg.getTpmUuid(), deletionMsg.getVmInstanceUuid()));
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                })
                // TODO: It should has rollback
                .build())
            .then(Flow.of("restore-vm-host-file")
                .handle(trigger -> {
                    RestoreVmHostFileMsg restoreMsg = new RestoreVmHostFileMsg();
                    restoreMsg.setVmInstanceUuid(vmUuid);
                    restoreMsg.setSnapshotGroupUuid(self.getUuid());
                    restoreMsg.setSyncReason(VmHostFileSyncReason.RevertSnapshot.reason());
                    bus.makeLocalServiceId(restoreMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                    bus.send(restoreMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                trigger.next();
                            } else {
                                trigger.fail(reply.getError());
                            }
                        }
                    });
                })
                .build())
            .then(Flow.of("revert-every-volumes")
                .handle(trigger -> {
                    final String finalNewGroupUuid = context.newGroup == null ? null : context.newGroup.getUuid();
                    new While<>(snapshots).each((snapshot, compl) -> {
                        if (Q.New(VolumeVO.class).eq(VolumeVO_.uuid, snapshot.getVolumeUuid()).eq(VolumeVO_.type, VolumeType.Memory).isExists()) {
                            compl.done();
                            return;
                        }

                        RevertVolumeFromSnapshotGroupMsg rmsg = new RevertVolumeFromSnapshotGroupMsg();
                        rmsg.setSnapshotUuid(snapshot.getUuid());
                        rmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        rmsg.setTreeUuid(snapshot.getTreeUuid());
                        rmsg.setSession(msg.getSession());
                        rmsg.setNewSnapshotGroupUuid(finalNewGroupUuid);

                        bus.makeTargetServiceIdByResourceUuid(rmsg, VolumeSnapshotConstant.SERVICE_ID, getResourceIdToRouteMsg(snapshot));
                        bus.send(rmsg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply r) {
                                reply.addResult(new RevertSnapshotGroupResult(rmsg.getSnapshotUuid(), rmsg.getVolumeUuid(), r.getError()));
                                compl.done();
                            }
                        });
                    }).run(new WhileDoneCompletion(msg) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            DebugUtils.Assert(!errorCodeList.hasError(), "no errorCode expected");
                            trigger.next();
                        }
                    });
                })
                .build())
            .then(Flow.of("delete-tpm-key-backup")
                .runIf(data -> context.tpmKeyBackupUuid != null)
                .handle(trigger -> {
                    DeleteTpmKeyBackupMsg delMsg = new DeleteTpmKeyBackupMsg();
                    delMsg.setTpmUuid(context.tpmUuid);
                    delMsg.setTpmKeyBackupUuid(context.tpmKeyBackupUuid);
                    bus.makeTargetServiceIdByResourceUuid(delMsg, SERVICE_ID, context.tpmUuid);
                    bus.send(delMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply r) {
                            if (r.isSuccess()) {
                                context.tpmKeyBackupUuid = null;
                                trigger.next();
                            } else {
                                trigger.fail(r.getError());
                            }
                        }
                    });
                })
                .build())
            .propagateExceptionTo(msg)
            .done(() -> bus.reply(msg, reply))
            .error(errorCode -> {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            })
            .start();
    }

    public List<VolumeSnapshotVO> getSnapshots() {
        return SQL.New("select snap from VolumeSnapshotGroupRefVO ref, VolumeSnapshotVO snap" +
                " where ref.volumeSnapshotGroupUuid = :groupUuid" +
                " and snap.uuid = ref.volumeSnapshotUuid", VolumeSnapshotVO.class)
                .param("groupUuid", self.getUuid())
                .list();
    }

    @Transactional(readOnly = true)
    public List<VolumeSnapshotVO> getEffectiveSnapshots() {
        List<VolumeSnapshotVO> snapshots = getSnapshots();
        if (!VolumeSnapshotGlobalConfig.EFFECTIVE_COUNT_WITH_DETACHED_VOLUMES.value(Boolean.class)) {
            Set<String> attachedVolUuids = new HashSet<>(Q.New(VolumeVO.class)
                    .eq(VolumeVO_.vmInstanceUuid, self.getVmInstanceUuid())
                    .select(VolumeVO_.uuid).listValues());
            snapshots.removeIf(it -> !attachedVolUuids.contains(it.getVolumeUuid()));
        }
        return snapshots;
    }

    private void overlaySend(VolumeSnapshotGroupMessage imsg, CloudBusCallBack callBack) {
        VolumeSnapshotGroupOverlayMsg omsg = new VolumeSnapshotGroupOverlayMsg();
        omsg.setVmInstanceUuid(self.getVmInstanceUuid());
        omsg.setMessage((NeedReplyMessage) imsg);
        bus.makeTargetServiceIdByResourceUuid(omsg, VmInstanceConstant.SERVICE_ID, self.getVmInstanceUuid());
        bus.send(omsg, callBack);
    }
}
