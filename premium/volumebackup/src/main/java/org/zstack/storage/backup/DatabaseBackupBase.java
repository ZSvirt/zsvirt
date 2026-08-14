package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.imagestore.GetSyncTaskStatusMsg;
import org.zstack.header.imagestore.GetSyncTaskStatusReply;
import org.zstack.header.imagestore.PushBitsBetweenImageStoreMsg;
import org.zstack.header.imagestore.PushBitsBetweenImageStoreReply;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.*;
import org.zstack.storage.backup.imagestore.DeleteExportedImageFromImageStoreBackupStorageMsg;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DatabaseBackupBase {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;

    protected DatabaseBackupVO self;
    private static CLogger logger = Utils.getLogger(DatabaseBackupBase.class);

    public DatabaseBackupBase(DatabaseBackupVO self) {
        this.self = self;
    }

    public DatabaseBackupBase(){}

    private String getSyncId() {
        return String.format("operate-database-backup-%s", self.getUuid());
    }

    private void checkStatusAndState(){
        if (self.getState() != DatabaseBackupState.Enabled || self.getStatus() != DatabaseBackupStatus.Ready) {
            throw new OperationFailureException(operr("database backup[uuid:%s] is not Enabled and Ready", self.getUuid()));
        }
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void handleLocalMessage(Message msg){
        if (msg instanceof SyncDatabaseBackupFromImageStoreBackupStorageMsg) {
            handle((SyncDatabaseBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof DatabaseBackupDeletionMsg) {
            handle((DatabaseBackupDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteExportedDatabaseBackupFromBackupStorageMsg) {
            handle((APIDeleteExportedDatabaseBackupFromBackupStorageMsg)msg);
        } else if (msg instanceof APIExportDatabaseBackupFromBackupStorageMsg){
            handle((APIExportDatabaseBackupFromBackupStorageMsg)msg);
        } else if (msg instanceof APISyncDatabaseBackupFromImageStoreBackupStorageMsg){
            handle((APISyncDatabaseBackupFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteDatabaseBackupMsg) {
            handle((APIDeleteDatabaseBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteDatabaseBackupMsg msg) {
        APIDeleteDatabaseBackupEvent event = new APIDeleteDatabaseBackupEvent(msg.getId());
        doDeleteDatabaseBackup(msg, new Completion(event) {
            @Override
            public void success() {
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(APISyncDatabaseBackupFromImageStoreBackupStorageMsg msg) {
        SyncDatabaseBackupFromImageStoreBackupStorageMsg smsg = new SyncDatabaseBackupFromImageStoreBackupStorageMsg();
        smsg.setUuid(msg.getUuid());
        smsg.setSrcBackupStorageUuid(msg.getSrcBackupStorageUuid());
        smsg.setDstBackupStorageUuid(msg.getDstBackupStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, DatabaseBackupConstant.SERVICE_ID, msg.getSrcBackupStorageUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                APISyncDatabaseBackupFromImageStoreBackupStorageEvent event = new APISyncDatabaseBackupFromImageStoreBackupStorageEvent(msg.getId());
                if (!r.isSuccess()) {
                    event.setError(r.getError());
                    bus.publish(event);
                    return;
                }

                SyncDatabaseBackupFromImageStoreBackupStorageReply reply = r.castReply();
                event.setInventory(reply.getInventory());
                bus.publish(event);
            }
        });
    }

    private void handle(APIExportDatabaseBackupFromBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIExportDatabaseBackupFromBackupStorageEvent evt = new APIExportDatabaseBackupFromBackupStorageEvent(msg.getId());
                exportDatabaseBackup(msg, new ReturnValueCompletion<String>(chain) {
                    @Override
                    public void success(String exportUrl) {
                        evt.setDatabaseBackupUrl(exportUrl);
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

    private void exportDatabaseBackup(APIExportDatabaseBackupFromBackupStorageMsg msg, ReturnValueCompletion<String> completion){
        checkStatusAndState();
        ExportImageFromBackupStorageMsg emsg = new ExportImageFromBackupStorageMsg();
        emsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        DatabaseBackupStorageRefVO vo = self.getBackupStorageRefs().stream()
                .filter(it -> it.getBackupStorageUuid().equals(msg.getBackupStorageUuid()))
                .findFirst().orElse(null);
        emsg.setRawPath(vo != null ? vo.getInstallPath() : null);
        emsg.setRequiredSize(self.getSize());
        emsg.setImageName(self.getName());
        bus.makeTargetServiceIdByResourceUuid(emsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
        bus.send(emsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                ExportImageFromBackupStorageReply r = reply.castReply();
                SQL.New(DatabaseBackupStorageRefVO.class).set(DatabaseBackupStorageRefVO_.exportUrl, r.getImageUrl())
                        .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                        .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                        .update();
                completion.success(r.getImageUrl());
            }
        });
    }

    private void handle(APIDeleteExportedDatabaseBackupFromBackupStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIDeleteExportedDatabaseBackupFromBackupStorageEvent evt = new APIDeleteExportedDatabaseBackupFromBackupStorageEvent(msg.getId());
                deleteExportDatabaseBackup(msg, new Completion(chain) {
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

    private void deleteExportDatabaseBackup(APIDeleteExportedDatabaseBackupFromBackupStorageMsg msg, Completion completion){
        DeleteExportedImageFromImageStoreBackupStorageMsg dmsg = new DeleteExportedImageFromImageStoreBackupStorageMsg();
        dmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        DatabaseBackupStorageRefVO vo = self.getBackupStorageRefs().stream()
                .filter(it -> it.getBackupStorageUuid().equals(msg.getBackupStorageUuid()))
                .findFirst().orElse(null);
        dmsg.setRawPath(vo != null ? vo.getInstallPath() : null);
        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                SQL.New(DatabaseBackupStorageRefVO.class).set(DatabaseBackupStorageRefVO_.exportUrl, null)
                        .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                        .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                        .update();
                completion.success();
            }
        });
    }

    private void handle(SyncDatabaseBackupFromImageStoreBackupStorageMsg msg) {
        checkStatusAndState();
        SyncDatabaseBackupFromImageStoreBackupStorageReply reply = new SyncDatabaseBackupFromImageStoreBackupStorageReply();

        if (Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.state, BackupStorageState.Disabled)
                .in(BackupStorageVO_.uuid, Arrays.asList(msg.getDstBackupStorageUuid(), msg.getSrcBackupStorageUuid()))
                .isExists()) {
            reply.setError(operr("One of the backup storage[uuids: %s, %s] is in the state of %s, can not do sync operation",
                    msg.getDstBackupStorageUuid(), msg.getSrcBackupStorageUuid(), BackupStorageState.Disabled.toString()));
            bus.reply(msg, reply);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSync(msg, new ReturnValueCompletion<DatabaseBackupInventory>(msg) {
                    @Override
                    public void success(DatabaseBackupInventory inv) {
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

    private void doSync(SyncDatabaseBackupMessage msg, ReturnValueCompletion<DatabaseBackupInventory> completion) {
        final String installPath = Q.New(DatabaseBackupStorageRefVO.class)
                .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getSrcBackupStorageUuid())
                .select(DatabaseBackupStorageRefVO_.installPath)
                .findValue();
        if (installPath == null) {
            completion.fail(Platform.operr("database backup[uuid:%s] not found in backup storage[uuid:%s]",
                    msg.getDatabaseBackupUuid(), msg.getSrcBackupStorageUuid()));
            return;
        }

        if (Q.New(DatabaseBackupStorageRefVO.class)
                .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getDstBackupStorageUuid()).isExists()) {
            DatabaseBackupVO dbvo = dbf.findByUuid(msg.getDatabaseBackupUuid(), DatabaseBackupVO.class);
            completion.success(DatabaseBackupInventory.valueOf(dbvo));
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("push--database-backup-%s-from-src-to-dst-imagestore", msg.getDatabaseBackupUuid()));
        chain.then(new ShareFlow() {
            DatabaseBackupStorageRefVO refVO;
            String taskId;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        refVO = new DatabaseBackupStorageRefVO();
                        refVO.setDatabaseBackupUuid(msg.getDatabaseBackupUuid());
                        refVO.setBackupStorageUuid(msg.getDstBackupStorageUuid());
                        refVO.setStatus(DatabaseBackupStatus.Creating);
                        refVO.setInstallPath(installPath);
                        refVO = dbf.persist(refVO);
                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "push-backup-" + msg.getDatabaseBackupUuid();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PushBitsBetweenImageStoreMsg pmsg = new PushBitsBetweenImageStoreMsg();
                        pmsg.setDstImageStorageUuid(msg.getDstBackupStorageUuid());
                        pmsg.setSrcImageStorageUuid(msg.getSrcBackupStorageUuid());
                        pmsg.setInstallPath(installPath);

                        bus.makeTargetServiceIdByResourceUuid(pmsg, ImageStoreBackupStorageConstant.SERVICE_ID, msg.getSrcBackupStorageUuid());
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
                        waitUntilReady(msg.getSrcBackupStorageUuid(), taskId, trigger);
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        refVO.setStatus(DatabaseBackupStatus.Ready);
                        dbf.updateAndRefresh(refVO);

                        DatabaseBackupVO dbvo = dbf.findByUuid(msg.getDatabaseBackupUuid(), DatabaseBackupVO.class);
                        completion.success(DatabaseBackupInventory.valueOf(dbvo));
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
                bus.makeTargetServiceIdByResourceUuid(gmsg, ImageStoreBackupStorageConstant.SERVICE_ID, bsUuid);

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

    private void handle(DatabaseBackupDeletionMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                DatabaseBackupDeletionReply reply = new DatabaseBackupDeletionReply();
                doDeleteDatabaseBackup(msg, new Completion(msg, chain) {
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
                return getSyncSignature();
            }
        });
    }

    private void doDeleteDatabaseBackup(DeleteDatabaseBackupMessage msg, Completion completion) {
        if (!dbf.isExist(msg.getDatabaseBackupUuid(), DatabaseBackupVO.class)) {
            completion.success();
            return;
        }

        List<DeleteBitsOnBackupStorageMsg> dmsgs = buildMsgs(msg.getDatabaseBackupUuid(), msg.getBackupStorageUuids());
        new While<>(dmsgs).all((dmsg, compl) -> {
            CloudBusCallBack callBack = new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().getReadableDetails());
                    }

                    SQL.New(DatabaseBackupStorageRefVO.class)
                            .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                            .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, dmsg.getBackupStorageUuid())
                            .eq(DatabaseBackupStorageRefVO_.installPath, dmsg.getInstallPath())
                            .delete();
                    compl.done();
                }
            };

            if (msg.isDbOnly()) {
                callBack.run(new MessageReply());
            } else {
                bus.send(dmsg, callBack);
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!Q.New(DatabaseBackupStorageRefVO.class)
                        .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                        .isExists()) {
                    dbf.removeByPrimaryKey(msg.getDatabaseBackupUuid(), DatabaseBackupVO.class);
                }
                afterDeleteDatabaseBackup();
                completion.success();
            }

            private void afterDeleteDatabaseBackup() {
                pluginRgty.getExtensionList(DeleteDatabaseBackupExtensionPoint.class).forEach(new Consumer<DeleteDatabaseBackupExtensionPoint>() {
                    @Override
                    @ExceptionSafe
                    public void accept(DeleteDatabaseBackupExtensionPoint ext) {
                        ext.afterDeleteDatabaseBackup(msg.getDatabaseBackupUuid(), msg.getBackupStorageUuids());
                    }
                });
            }
        });
    }

    private List<DeleteBitsOnBackupStorageMsg> buildMsgs(String backupUuid, List<String> backupStorageUuids) {
        Q q = Q.New(DatabaseBackupStorageRefVO.class)
                .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, backupUuid)
                .select(DatabaseBackupStorageRefVO_.installPath)
                .select(DatabaseBackupStorageRefVO_.backupStorageUuid);

        if (backupStorageUuids != null && !backupStorageUuids.isEmpty()) {
            q.in(DatabaseBackupStorageRefVO_.backupStorageUuid, backupStorageUuids);
        }

        List<Tuple> ts = q.listTuple();

        return ts.stream().map(t -> {
            DeleteBitsOnBackupStorageMsg dmsg = new DeleteBitsOnBackupStorageMsg();
            dmsg.setInstallPath(t.get(0, String.class));
            dmsg.setBackupStorageUuid(t.get(1, String.class));
            bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, dmsg.getBackupStorageUuid());
            return dmsg;
        }).collect(Collectors.toList());
    }
}
