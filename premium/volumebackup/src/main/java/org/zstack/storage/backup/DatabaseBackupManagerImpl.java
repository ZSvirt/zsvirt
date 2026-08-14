package org.zstack.storage.backup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.imagestore.ImageStoreReclaimSpaceExtensionPoint;
import org.zstack.header.imagestore.PullImageToLocalMsg;
import org.zstack.header.imagestore.PullImageToLocalReply;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.*;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagLifeCycleListener;
import org.zstack.mevoco.MevocoGlobalProperty;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.SshShell;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

public class DatabaseBackupManagerImpl extends AbstractService implements DatabaseBackupManager,
        ImageStoreReclaimSpaceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(DatabaseBackupManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private TagManager tagMgr;

    private Map<String, DatabaseRecoverChecker> databaseRecoverCheckerMap = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DatabaseBackupMessage) {
            passThrough((DatabaseBackupMessage)msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(DatabaseBackupMessage msg) {
        DatabaseBackupVO vo = dbf.findByUuid(msg.getDatabaseBackupUuid(), DatabaseBackupVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("database backup [uuid:%s] is not existed yet", msg.getDatabaseBackupUuid()));
        }
        new DatabaseBackupBase(vo).handleMessage((Message)msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateDatabaseBackupMsg) {
            handle((CreateDatabaseBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private ErrorCode checkStateAndStatus(String bsUuid){
        BackupStorageVO bs = dbf.findByUuid(bsUuid, BackupStorageVO.class);
        if (bs == null || bs.getState() != BackupStorageState.Enabled || bs.getStatus() != BackupStorageStatus.Connected) {
            return operr("backup storage[uuid:%s] is not enabled and connected");
        }
        return null;
    }

    private void handle(CreateDatabaseBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "take-backup-for-database";
            }

            @Override
            public void run(SyncTaskChain chain) {
                CreateDatabaseBackupReply r = new CreateDatabaseBackupReply();
                runCreateDatabaseBackupFlow(msg, new ReturnValueCompletion<DatabaseBackupInventory>(msg, chain) {
                    @Override
                    public void success(DatabaseBackupInventory inv) {
                        r.setInventory(inv);
                        bus.reply(msg, r);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        r.setError(errorCode);
                        bus.reply(msg, r);
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

    private void runCreateDatabaseBackupFlow(CreateDatabaseBackupMsg msg, ReturnValueCompletion<DatabaseBackupInventory> completion) {
        ErrorCode err = checkStateAndStatus(msg.getBackupStorageUuid());
        if (err != null) {
            completion.fail(err);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("take-database-backup");
        chain.then(new ShareFlow() {
            String uploadDir;
            DatabaseBackupMetadata metadata;
            ImportImageReply ireply;
            DatabaseBackupVO dbvo;
            ImageStoreBackupStorageVO ivo = dbf.findByUuid(msg.getBackupStorageUuid(), ImageStoreBackupStorageVO.class);
            DatabaseBackupVersionExtensionPoint ext = pluginRgty.getExtensionFromMap(
                    MevocoGlobalProperty.DEPLOY_MODE, DatabaseBackupVersionExtensionPoint.class);

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocateUploadWorkspaceMsg amsg = new AllocateUploadWorkspaceMsg();
                        amsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            AllocateUploadWorkspaceReply r = reply.castReply();
                                            uploadDir = r.getUploadWorkspace();
                                            trigger.next();
                                        } else {
                                            trigger.fail(reply.getError());
                                        }
                                    }
                                }
                        );
                    }
                });

                flow(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        dbvo = new DatabaseBackupVO();
                        dbvo.setUuid(Platform.getUuid());
                        dbvo.setName(msg.getName());
                        dbvo.setSize(0L);
                        dbvo.setDescription(msg.getDescription());
                        dbvo.setState(DatabaseBackupState.Disabled);
                        dbvo.setStatus(DatabaseBackupStatus.Creating);
                        dbvo = dbf.persistAndRefresh(dbvo);
                        tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), dbvo.getUuid(), DatabaseBackupVO.class.getSimpleName());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.removeByPrimaryKey(dbvo.getUuid(), DatabaseBackupVO.class);
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String tempDir;
                    String localBackupPath;

                    String __name__ = "upload-backup-to-bs";

                    private DatabaseBackupMetadata dumpDatabase(){
                        localBackupPath = PathUtil.join(tempDir, msg.getName());

                        String dump = String.format("timeout %d zstack-ctl dump_mysql --file-path %s >/dev/null && sudo md5sum %s",
                                msg.getTimeout(), localBackupPath, localBackupPath);

                        ShellResult result = ShellUtils.runAndReturn(dump);
                        result.raiseExceptionIfFail();
                        DatabaseBackupMetadata metadata = new DatabaseBackupMetadata();
                        metadata.name = msg.getName();
                        metadata.description = msg.getDescription();
                        metadata.version = ext == null ? dbf.getDbVersion() : ext.getVersion(dbf.getDbVersion());
                        metadata.md5 = result.getStdout().split("\\s+")[0];
                        metadata.type = getCurrentType();
                        metadata.createdTime = new Timestamp(System.currentTimeMillis());
                        return metadata;
                    }

                    @Deferred
                    private void scpToBs() {
                        String passFile = PathUtil.createTempFileWithContent(ivo.getPassword());
                        Defer.defer(() -> PathUtil.forceRemoveFile(passFile));
                        String cmd = String.format(
                                "sshpass -f '%s' scp -P %d -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null %s %s@%s:%s",
                                passFile, ivo.getSshPort(), localBackupPath, ivo.getUsername(), ivo.getHostname(), uploadDir);
                        ShellUtils.runAndReturn(cmd).raiseExceptionIfFail();
                    }

                    private void cleanEnv(){
                        if (!StringUtils.isEmpty(tempDir)) {
                            ShellUtils.runAndReturn(String.format("rm -rf %s", tempDir)).raiseExceptionIfFail();
                        }
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (CoreGlobalProperty.UNIT_TEST_ON) {
                            metadata = new DatabaseBackupMetadata();
                            metadata.version = ext == null ? dbf.getDbVersion() : ext.getVersion(dbf.getDbVersion());
                            metadata.name = DatabaseBackupConstant.buildDatabaseBackupName(metadata.version);
                            metadata.md5 = Platform.getUuid();
                            trigger.next();
                            return;
                        }

                        tempDir = PathUtil.createTempDirectory();
                        metadata = dumpDatabase();
                        scpToBs();
                        cleanEnv();
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        cleanEnv();
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "import-db-on-bs";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImportImageMsg imsg = new ImportImageMsg();
                        imsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        imsg.setParent(null);
                        imsg.setFilename("file://" + PathUtil.join(uploadDir, metadata.name));
                        imsg.setName(DatabaseBackupConstant.imageBackupName);
                        imsg.setDescription(JSONObjectUtil.toJsonString(metadata));
                        bus.makeTargetServiceIdByResourceUuid(imsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(imsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ireply = reply.castReply();
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        DatabaseBackupInventory inv = new SQLBatchWithReturn<DatabaseBackupInventory>() {
                            @Override
                            protected DatabaseBackupInventory scripts() {
                                dbvo.setState(DatabaseBackupState.Enabled);
                                dbvo.setStatus(DatabaseBackupStatus.Ready);
                                dbvo.setSize(ireply.getSize());
                                dbvo.setMetadata(JSONObjectUtil.toJsonString(metadata));
                                merge(dbvo);

                                DatabaseBackupStorageRefVO refVO = new DatabaseBackupStorageRefVO();
                                refVO.setBackupStorageUuid(msg.getBackupStorageUuid());
                                refVO.setDatabaseBackupUuid(dbvo.getUuid());
                                refVO.setInstallPath(ireply.getInstallPath());
                                refVO.setStatus(DatabaseBackupStatus.Ready);
                                persist(refVO);

                                dbvo.setBackupStorageRefs(Collections.singleton(refVO));
                                return DatabaseBackupInventory.valueOf(dbvo);
                            }
                        }.execute();

                        cleanUploadDir();
                        completion.success(inv);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        cleanUploadDir();
                        completion.fail(errCode);
                    }
                });
            }

            @ExceptionSafe
            private void cleanUploadDir(){
                SshShell sshShell = new SshShell();
                sshShell.setHostname(ivo.getHostname());
                sshShell.setUsername(ivo.getUsername());
                sshShell.setPassword(ivo.getPassword());
                sshShell.setPort(ivo.getSshPort());
                sshShell.runCommand(String.format("rm -rf %s", uploadDir));
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg){
        if (msg instanceof APIGetDatabaseBackupFromImageStoreMsg) {
            handle((APIGetDatabaseBackupFromImageStoreMsg) msg);
        } else if (msg instanceof APIRecoverDatabaseFromBackupMsg) {
            handle((APIRecoverDatabaseFromBackupMsg) msg);
        } else if (msg instanceof APICreateDatabaseBackupMsg) {
            handle((APICreateDatabaseBackupMsg) msg);
        } else if (msg instanceof APISyncDatabaseBackupMsg) {
            handle((APISyncDatabaseBackupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateDatabaseBackupMsg msg) {
        CreateDatabaseBackupMsg cmsg = new CreateDatabaseBackupMsg();
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setSystemTags(msg.getSystemTags());
        cmsg.addSystemTag(DatabaseBackupSystemTag.MANUAL_CREATE_RESOURCE.instantiateTag(Collections.singletonMap(VolumeBackupSystemTag.API_ID_TOKEN, msg.getId())));
        bus.makeTargetServiceIdByResourceUuid(cmsg, DatabaseBackupConstant.SERVICE_ID, msg.getBackupStorageUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                APICreateDatabaseBackupEvent event = new APICreateDatabaseBackupEvent(msg.getId());
                if (!r.isSuccess()) {
                    event.setError(r.getError());
                    bus.publish(event);
                    return;
                }

                CreateDatabaseBackupReply reply = r.castReply();
                event.setInventory(reply.getInventory());
                bus.publish(event);
            }
        });

    }

    private void handle(APIRecoverDatabaseFromBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "recover-database";
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIRecoverDatabaseFromBackupEvent evt = new APIRecoverDatabaseFromBackupEvent(msg.getId());
                prepareRecoverDatabaseFromBackup(msg, new ReturnValueCompletion<RecoverDatabaseInfo>(chain) {
                    @Override
                    public void success(RecoverDatabaseInfo info) {
                        tailLog(info.logLstenPort);
                        waitTailLogReady(info.logLstenPort);
                        evt.setLogListenPort(info.logLstenPort);
                        bus.publish(evt);
                        recoverDatabase(info);
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

            @AsyncThread
            private void tailLog(int logListenPort) {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    return;
                }

                String cmd = StringDSL.s("bash -c '",
                        "iptables -I INPUT -p tcp -m tcp --dport {0} -j ACCEPT;",
                        "iptables -I OUTPUT -p tcp -m tcp --sport {0} -j ACCEPT;",
                        "zstack-ctl taillog --listen-port {0} --protocol websocket --timeout 1800;",
                        "iptables -D INPUT -p tcp -m tcp --dport {0} -j ACCEPT;",
                        "iptables -D OUTPUT -p tcp -m tcp --sport {0} -j ACCEPT;",
                        "' &"
                ).format(logListenPort);
                ShellUtils.runAndReturn(cmd);
            }

            private void waitTailLogReady(Integer logListenPort) {
                for (int i = 0; i < 3; i++) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignore) {
                        Thread.currentThread().interrupt();
                    }

                    boolean started = ShellUtils.runAndReturn("lsof -i :" + logListenPort.toString()).isReturnCode(0);
                    if (started) {
                        logger.debug("web taillog has started.");
                        break;
                    }
                }
            }

            private void recoverDatabase(RecoverDatabaseInfo info){
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    return;
                }

                String cmd = StringDSL.s("zstack-ctl restore_mysql --mysql-root-password '{0}' -f {1} --skip-ui --skip-check 2>&1",
                        "| sudo sed -u 's/^/restore database\\[api={2}\\]: /' >> {3};",
                        "sudo rm -f {1}"
                ).format(info.mysqlRootPassword, info.bakPath, msg.getId(),
                        PathUtil.getFilePathUnderZStackHomeFolder("apache-tomcat/logs/management-server.log"));
                ShellUtils.runAndReturn(cmd).raiseExceptionIfFail();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });

    }

    @Override
    public List<String> getAvailableImageInstallPaths(String backupStorageUuid) {
        return Q.New(DatabaseBackupStorageRefVO.class).select(DatabaseBackupStorageRefVO_.installPath)
                .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, backupStorageUuid)
                .listValues();
    }

    class RecoverDatabaseInfo {
        String mysqlRootPassword;
        String bakPath;
        Integer logLstenPort;
    }

    private void prepareRecoverDatabaseFromBackup(APIRecoverDatabaseFromBackupMsg msg, ReturnValueCompletion<RecoverDatabaseInfo> completion){
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            RecoverDatabaseInfo info = new RecoverDatabaseInfo();
            DatabaseType type;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "validate-recover";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        type = getCurrentType();
                        databaseRecoverCheckerMap.get(type.toString()).check();
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "test-mysql-connection";

                    @Override
                    public boolean skip(Map data) {
                        return CoreGlobalProperty.UNIT_TEST_ON;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String cmd = String.format("mysql -u root --password='%s' -e 'exit'", msg.getMysqlRootPassword());
                        ShellUtils.runAndReturn(cmd).raiseExceptionIfFail();
                        info.mysqlRootPassword = msg.getMysqlRootPassword();
                        trigger.next();
                    }
                });

                if (msg.getUuid() != null) {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            DatabaseBackupVO vo = dbf.findByUuid(msg.getUuid(), DatabaseBackupVO.class);
                            DatabaseBackupMetadata metadata = JSONObjectUtil.toObject(vo.getMetadata(), DatabaseBackupMetadata.class);
                            checkVersion(metadata.version);
                            checkType(metadata.type);

                            List<ErrorCode> errs = Collections.synchronizedList(new ArrayList<>());
                            new While<>(vo.getBackupStorageRefs()).each((ref, compl) -> {
                                PullImageToLocalMsg pmsg = new PullImageToLocalMsg();
                                pmsg.setInstallPath(ref.getInstallPath());
                                pmsg.setBackupStorageUuid(ref.getBackupStorageUuid());
                                bus.makeLocalServiceId(pmsg, BackupStorageConstant.SERVICE_ID);
                                bus.send(pmsg, new CloudBusCallBack(compl) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            PullImageToLocalReply r = reply.castReply();
                                            info.bakPath = r.getLocalInstallPath();
                                            compl.allDone();
                                            return;
                                        }

                                        errs.add(reply.getError());
                                        compl.done();
                                    }
                                });
                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (errs.size() == vo.getBackupStorageRefs().size()) {
                                        trigger.fail(errs.get(0));
                                        return;
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "pull-database-backup-from-backup-storage";

                        @Override
                        public boolean skip(Map data) {
                            return CoreGlobalProperty.UNIT_TEST_ON;
                        }

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            //todo: support multi type bs
                            String cmd = String.format("zstack-ctl pull_database_backup --backup-storage-url %s --backup-install-path %s --json",
                                    msg.getBackupStorageUrl(), msg.getBackupInstallPath());
                            ShellResult result = ShellUtils.runAndReturn(cmd);
                            result.raiseExceptionIfFail();
                            DatabaseBackupStruct metadata = JSONObjectUtil.toObject(result.getStdout(), DatabaseBackupStruct.class);
                            info.bakPath = metadata.getInstallPath();

                            checkVersion(metadata.getVersion());
                            checkType(metadata.getType());
                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "check_restore_security";

                    @Override
                    public boolean skip(Map data) {
                        return CoreGlobalProperty.UNIT_TEST_ON;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String cmd = String.format("zstack-ctl check_restore_mysql --mysql-root-password '%s' -f %s ",
                                msg.getMysqlRootPassword(), info.bakPath);
                        ShellResult result = ShellUtils.runAndReturn(cmd);
                        if (result.getRetCode() != 0) {
                            throw new OperationFailureException(operr("not pass the restore security check:\n%s", result.getStderr()));
                        }
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get_listen_port";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        info.logLstenPort = WebUtils.getFreePort();
                        if (info.logLstenPort == null) {
                            trigger.fail(operr("cannot get free port to listen"));
                        } else {
                            trigger.next();
                        }
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(info);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (info.bakPath != null) {
                            PathUtil.forceRemoveFile(info.bakPath);
                        }

                        completion.fail(errCode);
                    }
                });
            }

            private void checkVersion(String version){
                DatabaseBackupVersionExtensionPoint ext = pluginRgty.getExtensionFromMap(
                        MevocoGlobalProperty.DEPLOY_MODE, DatabaseBackupVersionExtensionPoint.class);
                String dbVersion = ext == null ? dbf.getDbVersion() : ext.getVersion(dbf.getDbVersion());
                if (!version.equals(dbVersion)) {
                    throw new OperationFailureException(operr(
                            "database backup version[%s] is not match currently version[%s]", version, dbVersion));
                }
            }

            private void checkType(DatabaseType type){
                if (type != this.type) {
                    logger.warn(String.format("database backup type[%s] is not match currently type[%s]",
                            type, this.type));
                }
            }
        }).start();
    }

    private DatabaseType getCurrentType() {
        if (PathUtil.exists("/usr/local/bin/zsha2")) {
            return DatabaseType.multiDatabase;
        }

        return DatabaseType.singleDatabase;
    }

    private void handle(APIGetDatabaseBackupFromImageStoreMsg msg) {
        APIGetDatabaseBackupFromImageStoreReply reply = new APIGetDatabaseBackupFromImageStoreReply();
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            reply.setBackups(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        reply.setBackups(scanDatabaseBackup(msg.getUrl(), msg.getRegistryPort()));
        bus.reply(msg, reply);
    }

    protected List<DatabaseBackupStruct> scanDatabaseBackup(String url) {
        return scanDatabaseBackup(url, ImageStoreBackupStorageGlobalProperty.REGISTRY_PORT);
    }

    private List<DatabaseBackupStruct> scanDatabaseBackup(String url, int registryPort) {
        String cmd = String.format("zstack-ctl scan_database_backup --json --backup-storage-url %s -p %d", url, registryPort);
        ShellResult result = ShellUtils.runAndReturn(cmd);
        result.raiseExceptionIfFail();
        return JSONObjectUtil.toCollection(result.getStdout(), ArrayList.class, DatabaseBackupStruct.class);
    }

    private void handle(APISyncDatabaseBackupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "sync-database-backup";
            }

            @Override
            public void run(SyncTaskChain chain) {
                APISyncDatabaseBackupEvent event = new APISyncDatabaseBackupEvent(msg.getId());
                event.setResult(syncDatabaseBackup(msg.getImageStoreUuid()));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private SyncBackupResult syncDatabaseBackup(String bsUuid) {
        ImageStoreBackupStorageVO bs = Q.New(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.uuid, bsUuid).find();
        String password = null;
        try {
            password = java.net.URLEncoder.encode(bs.getPassword(), "utf-8");
        } catch (UnsupportedEncodingException ignored) {}
        String url = String.format("ssh://%s:%s@%s:%s%s", bs.getUsername(), password, bs.getHostname(), bs.getSshPort(), bs.getUrl());

        List<DatabaseBackupStruct> actualBackups = scanDatabaseBackup(url);
        List<DatabaseBackupVO> dbBackups = dbf.listAll(DatabaseBackupVO.class);

        return syncDatabaseBackupRecords(bsUuid, actualBackups, dbBackups);
    }

    @Transactional
    private SyncBackupResult syncDatabaseBackupRecords(String dstBsUuid, List<DatabaseBackupStruct> actualBackups, List<DatabaseBackupVO> dbBackups) {
        int deleteCount = 0;
        int newCount = 0;
        for (DatabaseBackupVO dbBackup : dbBackups) {
            if (dbBackup.getBackupStorageRefs() == null || dbBackup.getBackupStorageRefs().isEmpty()) {
                dbf.getEntityManager().remove(dbf.getEntityManager().merge(dbBackup));
                continue;
            }

            Optional<DatabaseBackupStruct> md5Match = actualBackups.stream().filter(bk ->
                    dbBackup.getMetadata().contains(bk.getMd5())).findFirst();

            Optional<DatabaseBackupStorageRefVO> refMatch = dbBackup.getBackupStorageRefs().stream()
                    .filter(ref -> ref.getBackupStorageUuid().equals(dstBsUuid)).findFirst();

            if (md5Match.isPresent()) {
                actualBackups.remove(md5Match.get());

                // persist miss ref
                if (!refMatch.isPresent()) {
                    DatabaseBackupStorageRefVO ref = new DatabaseBackupStorageRefVO();
                    ref.setDatabaseBackupUuid(dbBackup.getUuid());
                    ref.setBackupStorageUuid(dstBsUuid);
                    ref.setInstallPath(md5Match.get().getInstallPath());
                    ref.setStatus(DatabaseBackupStatus.Ready);
                    newCount++;
                    dbf.getEntityManager().persist(ref);
                }
            } else if (refMatch.isPresent()) {
                // remove backup record not existing
                dbf.getEntityManager().remove(dbf.getEntityManager().merge(refMatch.get()));
                deleteCount++;
                if (dbBackup.getBackupStorageRefs().size() <= 1) {
                    dbf.getEntityManager().remove(dbf.getEntityManager().merge(dbBackup));
                }
            }
        }

        // persist backup which not in db
        for (DatabaseBackupStruct it : actualBackups) {
            DatabaseBackupVO vo = buildRecordFromStruct(it);
            DatabaseBackupStorageRefVO ref = new DatabaseBackupStorageRefVO();
            ref.setDatabaseBackupUuid(vo.getUuid());
            ref.setBackupStorageUuid(dstBsUuid);
            ref.setInstallPath(it.getInstallPath());
            ref.setStatus(DatabaseBackupStatus.Ready);
            dbf.getEntityManager().persist(vo);
            dbf.getEntityManager().persist(ref);
            newCount++;
        }

        return new SyncBackupResult(deleteCount, newCount);
    }

    private DatabaseBackupVO buildRecordFromStruct(DatabaseBackupStruct struct) {
        DatabaseBackupVO vo = new DatabaseBackupVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(struct.getName());
        vo.setSize(struct.getSize());
        vo.setState(DatabaseBackupState.Enabled);
        vo.setStatus(DatabaseBackupStatus.Ready);
        vo.setCreateDate(struct.getCreatedTime());
        DatabaseBackupMetadata metadata = new DatabaseBackupMetadata();
        metadata.type = struct.getType();
        metadata.version = struct.getVersion();
        metadata.createdTime = struct.getCreatedTime();
        metadata.name = struct.getName();
        metadata.md5 = struct.getMd5();
        vo.setMetadata(JSONObjectUtil.toJsonString(metadata));
        return vo;
    }

    @Override
    public String getId() {
        return  bus.makeLocalServiceId(DatabaseBackupConstant.SERVICE_ID);
    }

    private void initSystemTags() {
        SystemTagLifeCycleListener listener = new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {

            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                List<String> databaseBackupUuids = Q.New(DatabaseBackupStorageRefVO.class)
                        .select(DatabaseBackupStorageRefVO_.databaseBackupUuid)
                        .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, tag.getResourceUuid())
                        .listValues();

                logger.debug(String.format("image store[uuid: %s] was no longer used for backup, delete %d db backup(s) on it",
                        tag.getResourceUuid(), databaseBackupUuids.size()));
                for (String databaseBackupUuid : databaseBackupUuids) {
                    DatabaseBackupDeletionMsg msg = new DatabaseBackupDeletionMsg();
                    msg.setUuid(databaseBackupUuid);
                    msg.setBackupStorageUuids(Collections.singletonList(tag.getResourceUuid()));
                    msg.setDbOnly(true);
                    bus.makeTargetServiceIdByResourceUuid(msg, DatabaseBackupConstant.SERVICE_ID, databaseBackupUuid);
                    bus.send(msg);
                }
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }
        };
        BackupHelper.installLifeCycleListener(listener);
    }

    @Override
    public boolean start() {
        SQL.New(DatabaseBackupVO.class).eq(DatabaseBackupVO_.status, DatabaseBackupStatus.Creating).delete();

        for (DatabaseRecoverChecker c : pluginRgty.getExtensionList(DatabaseRecoverChecker.class)) {
            DatabaseRecoverChecker old = databaseRecoverCheckerMap.get(c.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate DatabaseRecoverChecker for type[%s]",c.getType()));
            }

            databaseRecoverCheckerMap.put(c.getType(), c);
        }

        initSystemTags();

        pluginRgty.saveExtensionAsMap(DatabaseBackupVersionExtensionPoint.class, new Function<Object, DatabaseBackupVersionExtensionPoint>() {
            @Override
            public Object call(DatabaseBackupVersionExtensionPoint arg) {
                return arg.getDeployMode();
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
