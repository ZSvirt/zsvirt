package org.zstack.storage.snapshot;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.DataIntegrityViolationException;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.IncreasePrimaryStorageCapacityMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus.StatusEvent;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import javax.persistence.PersistenceException;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeSnapshotBase implements VolumeSnapshot {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotBase.class);
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    public VolumeSnapshotBase(VolumeSnapshotVO self) {
        this.self = self;
    }

    protected VolumeSnapshotVO self;

    protected VolumeSnapshotInventory getSelfInventory() {
        return VolumeSnapshotInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        handleLocalMessage(msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VolumeSnapshotPrimaryStorageDeletionMsg) {
            handle((VolumeSnapshotPrimaryStorageDeletionMsg) msg);
//        } else if (msg instanceof BackupVolumeSnapshotMsg) {
//            handle((BackupVolumeSnapshotMsg) msg);
//        } else if (msg instanceof VolumeSnapshotBackupStorageDeletionMsg) {
//            handle((VolumeSnapshotBackupStorageDeletionMsg) msg);
        } else if (msg instanceof ChangeVolumeSnapshotStatusMsg) {
            handle((ChangeVolumeSnapshotStatusMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ChangeVolumeSnapshotStatusMsg msg) {
        StatusEvent evt = StatusEvent.valueOf(msg.getEvent());
        changeStatus(evt);

        ChangeVolumeSnapshotStatusReply reply = new ChangeVolumeSnapshotStatusReply();
        bus.reply(msg, reply);
    }

/*
    private void handle(final VolumeSnapshotBackupStorageDeletionMsg msg) {
        final VolumeSnapshotBackupStorageDeletionReply reply = new VolumeSnapshotBackupStorageDeletionReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-from-backup-storage", self.getUuid()));

        for (final String bsUuid : msg.getBackupStorageUuids()) {
            final String installPath = CollectionUtils.find(self.getBackupStorageRefs(), new Function<String, VolumeSnapshotBackupStorageRefVO>() {
                @Override
                public String call(VolumeSnapshotBackupStorageRefVO arg) {
                    return arg.getBackupStorageUuid().equals(bsUuid) ? arg.getInstallPath() : null;
                }
            });

            DebugUtils.Assert(installPath!=null, String.format("why installPath is NULL for snapshot[uuid:%s] on backupStorage[uuid:%s]???", self.getUuid(), bsUuid));

            chain.then(new ShareFlow() {
                boolean success;

                @Override
                public void setup() {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-on-backup-storage-%s", bsUuid);
                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
                            dmsg.setInstallPath(installPath);
                            dmsg.setBackupStorageUuid(bsUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                            bus.send(dmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    success = reply.isSuccess();
                                    if (!success) {
                                        logger.warn(String.format("failed to delete snapshot[%s] on backup storage[uuid:%s], %s. the backup storage must clean up",
                                                self.getUuid(), bsUuid, reply.getError()));
                                    }

                                    VolumeSnapshotBackupStorageRefVO ref = CollectionUtils.find(self.getBackupStorageRefs(), new Function<VolumeSnapshotBackupStorageRefVO, VolumeSnapshotBackupStorageRefVO>() {
                                        @Override
                                        public VolumeSnapshotBackupStorageRefVO call(VolumeSnapshotBackupStorageRefVO arg) {
                                            return arg.getBackupStorageUuid().equals(bsUuid) ? arg : null;
                                        }
                                    });

                                    dbf.remove(ref);
                                    trigger.next();
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("return-capacity-to-backup-storage-%s", bsUuid);

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            if (!success) {
                                logger.warn(String.format("failed to delete volume snapshot[uuid:%s] from backup storage[uuid:%s], skip to return capacity[%s bytes]",
                                        self.getUuid(), bsUuid, self.getSize()));
                                trigger.next();
                                return;
                            }

                            ReturnBackupStorageMsg rmsg = new ReturnBackupStorageMsg();
                            rmsg.setSize(self.getSize());
                            rmsg.setBackupStorageUuid(bsUuid);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                            bus.send(rmsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to return capacity to backup storage[uuid:%s] for volume snapshot[uuid:%s, size:%s], %s",
                                                bsUuid, self.getUuid(), self.getSize(), reply.getError()));
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(final BackupVolumeSnapshotMsg msg) {
        final BackupVolumeSnapshotReply reply = new BackupVolumeSnapshotReply();
        BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg bmsg = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg();
        bmsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
        bmsg.setBackupStorage(msg.getBackupStorage());
        bmsg.setSnapshot(getSelfInventory());
        bus.makeTargetServiceIdByResourceUuid(bmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
        bus.send(bmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rsp) {
                if (!rsp.isSuccess()) {
                    reply.setError(rsp.getError());
                } else {
                    BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply r = rsp.castReply();
                    VolumeSnapshotBackupStorageRefVO ref = new VolumeSnapshotBackupStorageRefVO();
                    ref.setInstallPath(r.getBackupStorageInstallPath());
                    ref.setVolumeSnapshotUuid(self.getUuid());
                    ref.setBackupStorageUuid(msg.getBackupStorage().getUuid());
                    dbf.persist(ref);
                }

                bus.reply(msg, reply);
            }
        });
    }
*/

    private void changeStatus(VolumeSnapshotStatus.StatusEvent event) {
        self.setStatus(self.getStatus().nextState(event));
        dbf.update(self);
    }

    private void handle(final VolumeSnapshotPrimaryStorageDeletionMsg msg) {
        final VolumeSnapshotInventory sp = getSelfInventory();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-snapshot-%s-on-primary-storage", self.getUuid()));
        chain.then(new ShareFlow() {
            private void finish() {
                new While<>(pluginRgty.getExtensionList(VolumeSnapshotAfterDeleteExtensionPoint.class)).all((ext, c) -> {
                    ext.volumeSnapshotAfterDeleteExtensionPoint(sp, new Completion(c) {
                        @Override
                        public void success() {
                            c.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            c.done();
                        }
                    });
                }).run(new WhileDoneCompletion(msg) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        VolumeSnapshotPrimaryStorageDeletionReply dreply = new VolumeSnapshotPrimaryStorageDeletionReply();
                        try {
                            updateDb();
                        } catch (DataIntegrityViolationException | PersistenceException e) {
                            if (!ExceptionDSL.isCausedBy(e, ConstraintViolationException.class) && !ExceptionDSL.isCausedBy(e, DataIntegrityViolationException.class)) {
                                throw e;
                            }
                            // volume snapshot group may has been removed, try again.
                            updateDb();
                        }
                        bus.reply(msg, dreply);
                    }

                    private void updateDb() {
                        self = dbf.reload(self);
                        self.setPrimaryStorageInstallPath(null);
                        self.setPrimaryStorageUuid(null);
                        dbf.update(self);
                    }
                });
            }

            private void errors(ErrorCode errorCode) {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(VolumeSnapshotAfterDeleteExtensionPoint.class), new ForEachFunction<VolumeSnapshotAfterDeleteExtensionPoint>() {
                    @Override
                    public void run(VolumeSnapshotAfterDeleteExtensionPoint arg) {
                        arg.volumeSnapshotAfterFailedDeleteExtensionPoint(sp);
                    }
                });

                VolumeSnapshotPrimaryStorageDeletionReply dreply = new VolumeSnapshotPrimaryStorageDeletionReply();
                dreply.setError(errorCode);
                bus.reply(msg, dreply);
            }

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-on-primary-storage";
                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        DeleteSnapshotOnPrimaryStorageMsg dmsg = new DeleteSnapshotOnPrimaryStorageMsg();
                        dmsg.setSnapshot(getSelfInventory());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, self.getPrimaryStorageUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.setError(reply.getError());
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-primary-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        IncreasePrimaryStorageCapacityMsg imsg = new IncreasePrimaryStorageCapacityMsg();
                        imsg.setDiskSize(self.getSize());
                        imsg.setNoOverProvisioning(true);
                        imsg.setPrimaryStorageUuid(self.getPrimaryStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(imsg, PrimaryStorageConstant.SERVICE_ID, imsg.getPrimaryStorageUuid());
                        bus.send(imsg);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        finish();
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        // TODO GC
                        if (VolumeSnapshotErrors.FULL_SNAPSHOT_ERROR.toString().equals(errCode.getCode())) {
                            errors(errCode);
                        } else {
                            logger.warn(String.format("failed to delete snapshot[uuid:%s, name:%s] on primary storage[uuid:%s], the primary storage should cleanup",
                                    self.getUuid(), self.getName(), self.getPrimaryStorageUuid()));
                            finish();
                        }
                    }
                });
            }
        }).start();
    }
}
