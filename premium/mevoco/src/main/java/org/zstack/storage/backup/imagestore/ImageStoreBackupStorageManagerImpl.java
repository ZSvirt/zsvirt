package org.zstack.storage.backup.imagestore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.CallBackNetworkChecker;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorType;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.image.*;
import org.zstack.header.imagestore.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.DatabaseBackupStatus;
import org.zstack.header.storage.database.backup.DatabaseBackupStorageRefVO;
import org.zstack.header.storage.database.backup.DatabaseBackupStorageRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vo.FindSameNodeExtensionPoint;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageSystemTags;
import org.zstack.kvm.*;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant.CLEAN_IMAGESTORE_LOCAL_CACHE;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class ImageStoreBackupStorageManagerImpl extends AbstractService
        implements ImageStoreBackupStorageManager, ManagementNodeReadyExtensionPoint,
        KVMHostConnectExtensionPoint, FindSameNodeExtensionPoint, KVMBlockCommitExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected ResourceConfigFacade rcf;
    @Autowired
    private ImageStoreBackupStorageMetaDataMaker metaDataMaker;

    // Zstack store client & server shares the same installer package
    static private final String agentPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;
    static private final String agentClientPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_CLIENT_PACKAGE_NAME;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SyncImageBetweenImageStoreMsg) {
            handle((SyncImageBetweenImageStoreMsg) msg);
        } else if (msg instanceof SyncVolumeBackupBetweenImageStoreMsg) {
            handle((SyncVolumeBackupBetweenImageStoreMsg) msg);
        } else if (msg instanceof SyncDatabaseBackupBetweenImageStoreMsg) {
            handle((SyncDatabaseBackupBetweenImageStoreMsg) msg);
        } else if (msg instanceof RecoveryImageBetweenImageStoreMsg) {
            handle((RecoveryImageBetweenImageStoreMsg) msg);
        } else if (msg instanceof PushBitsBetweenImageStoreMsg) {
            handle((PushBitsBetweenImageStoreMsg) msg);
        } else if (msg instanceof PullBitsBetweenImageStoreMsg) {
            handle((PullBitsBetweenImageStoreMsg) msg);
        } else if (msg instanceof GetSyncTaskStatusMsg) {
            handle((GetSyncTaskStatusMsg) msg);
        } else if (msg instanceof CleanImageStoreLocalCacheMsg) {
            handle((CleanImageStoreLocalCacheMsg) msg);
        } else if (msg instanceof SyncImageFromImageStoreBackupStorageMsg) {
            handle((SyncImageFromImageStoreBackupStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CleanImageStoreLocalCacheMsg msg) {
        CleanImageStoreLocalCacheReply reply = new CleanImageStoreLocalCacheReply();
        ImageStoreBackupStorageCommands.CleanLocalImageStoreCacheCmd cmd =
                new ImageStoreBackupStorageCommands.CleanLocalImageStoreCacheCmd();
        cmd.mountPath = msg.getMountPath();
        new KvmCommandSender(msg.getHostUuid()).send(cmd, CLEAN_IMAGESTORE_LOCAL_CACHE, new KvmCommandFailureChecker() {
            @Override
            public ErrorCode getError(KvmResponseWrapper wrapper) {
                ImageStoreBackupStorageCommands.CleanLocalImageStoreCacheRsp rsp =
                        wrapper.getResponse(ImageStoreBackupStorageCommands.CleanLocalImageStoreCacheRsp.class);
                return rsp.isSuccess() ? null : operr("%s", rsp.getError());
            }
        }, new ReturnValueCompletion<KvmResponseWrapper>(msg) {
            @Override
            public void success(KvmResponseWrapper w) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetSyncTaskStatusMsg msg) {
        String hostName = Q.New(ImageStoreBackupStorageVO.class)
                .select(ImageStoreBackupStorageVO_.hostname)
                .eq(ImageStoreBackupStorageVO_.uuid, msg.getBsUuid())
                .findValue();

        GetSyncTaskStatusReply res = new GetSyncTaskStatusReply();
        ConnectTaskReply reply = connectHttps(hostName,
                String.format("%s/%s", ImageStoreBackupStorageGlobalProperty.REGISTRY_TASK_PATH, msg.getTaskId()),
                JSONObjectUtil.toJsonString(new ImageStoreBackupStorageCommands.AgentCommand()), true);
        if (reply.isSuccess()) {
            SyncTaskStatus status = SyncTaskStatus.get(reply.getValue());
            res.setStatus(status);
        } else {
            res.setError(reply.getError());
        }

        bus.reply(msg, res);
    }

    private void waitUntilReady(final String taskId, final String imageUuid, final String dstUuid, final String bsUuid, FlowTrigger trigger) {
        String hostName = Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.hostname).
                eq(ImageStoreBackupStorageVO_.uuid, bsUuid).findValue();

        final long interval = 2;

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
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
                return String.format("wait-until-task-ready-for-image-%s, taskid-%s", imageUuid, taskId);
            }

            @Override
            public boolean run() {
                ConnectTaskReply reply = connectHttps(hostName, String.format("%s/%s", ImageStoreBackupStorageGlobalProperty.REGISTRY_TASK_PATH, taskId),
                        JSONObjectUtil.toJsonString(new ImageStoreBackupStorageCommands.AgentCommand()), true);
                if (reply.isSuccess()) {
                    updateSyncSystemTags(bsUuid, imageUuid, dstUuid, taskId, reply.getValue());
                    if (SyncTaskStatus.TsRunning.toString().equals(reply.getValue()) ||
                            SyncTaskStatus.TsWaiting.toString().equals(reply.getValue())) {
                        logger.debug(String.format("sync task status: %s, wait %s seconds", reply.getValue(), interval));
                        // continue waiting...
                        return false;
                    } else if (SyncTaskStatus.TsFailed.toString().equals(reply.getValue())) {
                        trigger.fail(operr("sync status failed."));
                    } else {
                        trigger.next();
                    }
                    return true;
                } else {
                    trigger.fail(reply.getError());
                    return true;
                }
            }
        });
    }

    private String getOutputStream(BufferedReader reader) throws IOException {
        StringBuilder buf = new StringBuilder();
        String tmp = reader.readLine();
        while (tmp != null) {
            buf.append(tmp);
            tmp = reader.readLine();
        }
        logger.debug(String.format("get output stream: %s", buf.toString()));
        return buf.toString();
    }

    @Override
    public ResourceInventory findSameNode(String hostname) {
        String uuid = Q.New(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.hostname, hostname)
                .select(ImageStoreBackupStorageVO_.uuid).findValue();
        if (uuid == null) {
            return null;
        } else {
            ResourceInventory info = new ResourceInventory();
            info.setUuid(uuid);
            info.setResourceType(BackupStorageVO.class.getSimpleName());
            return info;
        }
    }

    @Override
    public void beforeCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, Completion completion) {
        completion.success();
    }

    @Override
    public void afterCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, CommitVolumeSnapshotOnHypervisorReply reply, Completion completion) {
        CleanImageMetaOnPrimaryStorageMsg cmsg = new CleanImageMetaOnPrimaryStorageMsg();
        cmsg.setVolumeUuid(msg.getVolume().getUuid());
        cmsg.setPrimaryStorageInstallPath(cmd.getBase());
        cmsg.setPsUuid(msg.getVolume().getPrimaryStorageUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, cmsg.getPsUuid());
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr(String.format("failed to clean image meta on primary storage %s, error: %s",
                            msg.getVolume().getPrimaryStorageUuid(), reply.getError())));
                    // TODO add gc
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void failedToCommitVolume(KVMHostInventory host, CommitVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockCommitCmd cmd, KVMAgentCommands.BlockCommitResponse rsp, ErrorCode err) {

    }

    class syncStatus {
        long lastOpTime;
        String status;

        public long getLastOpTime() {
            return lastOpTime;
        }

        public void setLastOpTime(long lastOpTime) {
            this.lastOpTime = lastOpTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public syncStatus(long lastOpTime, String status) {
            this.lastOpTime = lastOpTime;
            this.status = status;
        }
    }

    @Deferred
    private ConnectTaskReply connectHttps(final String hostName, final String path, final String data, boolean isTask) {
        ConnectTaskReply reply = new ConnectTaskReply();
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            if (isTask) {
                reply.setValue(SyncTaskStatus.TsSuccess.toString());
            } else {
                reply.setValue("taskid");
            }
            return reply;
        }

        try {
            InputStream ins = new ByteArrayInputStream(data.getBytes());
            HttpsConnectionHelper.trustAllHosts();
            HttpsURLConnection conn = (HttpsURLConnection) new URL(buildUrl(hostName, ImageStoreBackupStorageGlobalProperty.REGISTRY_PORT, path)).
                    openConnection();
            conn.setHostnameVerifier(HttpsConnectionHelper.DO_NOT_VERIFY);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            if (isTask) {
                conn.setRequestMethod("GET");
            } else {
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                OutputStream ous = conn.getOutputStream();
                IOUtils.copy(ins, ous);
                ous.close();
            }

            Defer.defer(conn::disconnect);

            int respCode = conn.getResponseCode();
            if (respCode == HttpsURLConnection.HTTP_ACCEPTED || respCode == HttpsURLConnection.HTTP_OK) {
                String result = "";
                if (isTask) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    syncStatus st = JSONObjectUtil.toObject(getOutputStream(reader), syncStatus.class);
                    if (st != null) {
                        result = st.getStatus();
                    }
                    reader.close();
                } else {
                    result = conn.getHeaderField(ImageStoreBackupStorageConstant.RESPONSE_TASK_ID_STR);
                }
                reply.setValue(result);
            } else {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    String errMsg = getOutputStream(err);
                    reply.setError(Platform.operr(errMsg));
                } catch (NullPointerException ne) {
                    reply.setError(Platform.operr("failed to get task reply!"));
                }
            }
            return reply;
        } catch (IOException e) {
            reply.setError(Platform.operr(e.getMessage()));
            return reply;
        }
    }

    private void handle(final RecoveryImageBetweenImageStoreMsg msg) {
        RecoveryImageBetweenImageStoreReply reply = new RecoveryImageBetweenImageStoreReply();

        String installPath = Q.New(ImageBackupStorageRefVO.class).
                eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getSrcImageStorageUuid()).
                eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid()).select(ImageBackupStorageRefVO_.installPath).findValue();

        PullBitsBetweenImageStoreMsg pmsg = new PullBitsBetweenImageStoreMsg();
        pmsg.setSrcImageStorageUuid(msg.getSrcImageStorageUuid());
        pmsg.setDstImageStorageUuid(msg.getDstImageStorageUuid());
        pmsg.setInstallPath(installPath);
        bus.makeLocalServiceId(pmsg, ImageStoreBackupStorageConstant.SERVICE_ID);

        bus.send(pmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (r.isSuccess()) {
                    ImageBackupStorageRefVO ref = new ImageBackupStorageRefVO();
                    ref.setBackupStorageUuid(msg.getDstImageStorageUuid());
                    ref.setImageUuid(msg.getNewImageUuid());
                    ref.setInstallPath(installPath);
                    ref.setStatus(ImageStatus.Ready);
                    dbf.persistAndRefresh(ref);

                    PullBitsBetweenImageStoreReply pr = r.castReply();
                    reply.setTaskId(pr.getTaskId());
                } else {
                    reply.setError(r.getError());
                }

                bus.reply(msg, reply);
            }
        });
    }

    private String buildUrl(String hostName, Integer port, String subPath) {
        String url = String.format("https://%s:%s%s", hostName, port, subPath);
        logger.debug(String.format("build url: %s", url));
        return url;
    }

    @Deferred
    private String getLocalCerts(String bsUuid) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "certs";
        }

        ImageStoreBackupStorageVO svo = dbf.findByUuid(bsUuid, ImageStoreBackupStorageVO.class);

        GLock lock = new GLock(String.format("lock-local-file-%s", svo.getHostname()), TimeUnit.SECONDS.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        return sshGetCert(svo);
    }

    private String sshGetCert(ImageStoreBackupStorageVO svo) {
        final String script = String.format("cat %s", ImageStoreBackupStorageGlobalProperty.REGISTRY_CERTS);

        final SshResult res = new Ssh().shell(script).setTimeout(45)
                .setPrivateKey(asf.getPrivateKey())
                .setUsername(svo.getUsername()).setPassword(svo.getPassword())
                .setHostname(svo.getHostname()).setPort(svo.getSshPort())
                .runAndClose();
        res.raiseExceptionIfFailed();
        return res.getStdout();
    }

    private void handle(final SyncVolumeBackupBetweenImageStoreMsg msg) {
        SyncVolumeBackupBetweenImageStoreReply reply = new SyncVolumeBackupBetweenImageStoreReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("do-sync-volume-backup-%s", msg.getVolumeBackupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncVolumeBackupBetweenImageStore(msg, new Completion(chain, msg) {
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
                return String.format("do-sync-volume-backup-%s-from-bs-%s-to-%s",
                        msg.getVolumeBackupUuid(),
                        msg.getSrcImageStorageUuid(),
                        msg.getDstImageStorageUuid());
            }
        });
    }

    private void handle(final SyncDatabaseBackupBetweenImageStoreMsg msg) {
        SyncDatabaseBackupBetweenImageStoreReply reply = new SyncDatabaseBackupBetweenImageStoreReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("do-sync-database-backup-%s", msg.getDatabaseBackupUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncDatabaseBackupBetweenImageStore(msg, new Completion(chain, msg) {
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
                return String.format("do-sync-database-backup-%s-from-bs-%s-to-%s",
                        msg.getDatabaseBackupUuid(),
                        msg.getSrcImageStorageUuid(),
                        msg.getDstImageStorageUuid());
            }
        });
    }

    private void handle(final SyncImageBetweenImageStoreMsg msg) {
        SyncImageBetweenImageStoreReply reply = new SyncImageBetweenImageStoreReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("do-sync-image-%s", msg.getImageUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncImageBetweenImageStore(msg, new Completion(chain, msg) {
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
                return String.format("do-sync-image-%s-from-bs-%s-to-%s",
                        msg.getImageUuid(),
                        msg.getSrcImageStorageUuid(),
                        msg.getDstImageStorageUuid());
            }
        });
    }

    private void doPushBits(String srcImageStoreUuid,
                            String dstImageStoreUuid,
                            String resourceUuid,
                            String targetResourceUuid,
                            String installPath,
                            Completion completion) {
        PushBitsBetweenImageStoreMsg pmsg = new PushBitsBetweenImageStoreMsg();
        pmsg.setSrcImageStorageUuid(srcImageStoreUuid);
        pmsg.setDstImageStorageUuid(dstImageStoreUuid);
        pmsg.setInstallPath(installPath);
        bus.makeLocalServiceId(pmsg, ImageStoreBackupStorageConstant.SERVICE_ID);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("sync-resource-%s-from-src-to-dst-imagestore", resourceUuid));

        chain.then(new ShareFlow() {
            String taskId;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "sync image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bus.send(pmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (r.isSuccess()) {
                                    PushBitsBetweenImageStoreReply pr = r.castReply();
                                    taskId = pr.getTaskId();
                                    trigger.next();
                                } else {
                                    trigger.fail(r.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "wait until success";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (taskId.equals(ImageStoreBackupStorageConstant.RESPONSE_TASK_EXISTED_FLAG)) {
                            updateSyncSystemTags(dstImageStoreUuid, resourceUuid, targetResourceUuid, taskId, SyncTaskStatus.TsSuccess.toString());
                            trigger.next();
                        } else {
                            waitUntilReady(taskId, resourceUuid, targetResourceUuid, srcImageStoreUuid, trigger);
                        }
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

    private void doSyncVolumeBackupBetweenImageStore(final SyncVolumeBackupBetweenImageStoreMsg msg, Completion completion) {
        String installPath = Q.New(VolumeBackupStorageRefVO.class)
                .eq(VolumeBackupStorageRefVO_.backupStorageUuid, msg.getSrcImageStorageUuid())
                .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, msg.getVolumeBackupUuid())
                .select(VolumeBackupStorageRefVO_.installPath)
                .findValue();

        VolumeBackupStorageRefVO ref = new SQLBatchWithReturn<VolumeBackupStorageRefVO>() {
            @Override
            protected VolumeBackupStorageRefVO scripts() {
                VolumeBackupStorageRefVO r = q(VolumeBackupStorageRefVO.class)
                        .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, msg.getVolumeBackupUuid())
                        .eq(VolumeBackupStorageRefVO_.backupStorageUuid, msg.getDstImageStorageUuid())
                        .orderBy(VolumeBackupStorageRefVO_.id, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();
                if (r != null) {
                    sql(VolumeBackupStorageRefVO.class)
                            .eq(VolumeBackupStorageRefVO_.volumeBackupUuid, msg.getVolumeBackupUuid())
                            .eq(VolumeBackupStorageRefVO_.backupStorageUuid, msg.getDstImageStorageUuid())
                            .lt(VolumeBackupStorageRefVO_.id, r.getId())
                            .delete();
                    return r;
                }

                VolumeBackupStorageRefVO newRef = new VolumeBackupStorageRefVO();
                newRef.setBackupStorageUuid(msg.getDstImageStorageUuid());
                newRef.setVolumeBackupUuid(msg.getNewVolumeBackupUuid());
                newRef.setInstallPath(installPath);
                newRef.setStatus(VolumeBackupStatus.Downloading);
                return persist(newRef);
            }
        }.execute();

        if (ref.getStatus().equals(VolumeBackupStatus.Ready)) {
            completion.success();
            return;
        }

        doPushBits(msg.getSrcImageStorageUuid(),
                msg.getDstImageStorageUuid(),
                msg.getVolumeBackupUuid(),
                msg.getNewVolumeBackupUuid(),
                installPath, new Completion(completion) {
                    @Override
                    public void success() {
                        ref.setStatus(VolumeBackupStatus.Ready);
                        dbf.updateAndRefresh(ref);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        dbf.remove(ref);
                        completion.fail(errorCode);
                    }
                });
    }

    private void doSyncDatabaseBackupBetweenImageStore(final SyncDatabaseBackupBetweenImageStoreMsg msg, Completion completion) {
        String installPath = Q.New(DatabaseBackupStorageRefVO.class)
                .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getSrcImageStorageUuid())
                .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                .select(DatabaseBackupStorageRefVO_.installPath)
                .findValue();

        DatabaseBackupStorageRefVO ref = new SQLBatchWithReturn<DatabaseBackupStorageRefVO>() {
            @Override
            protected DatabaseBackupStorageRefVO scripts() {
                DatabaseBackupStorageRefVO r = q(DatabaseBackupStorageRefVO.class)
                        .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                        .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getDstImageStorageUuid())
                        .orderBy(DatabaseBackupStorageRefVO_.id, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();
                if (r != null) {
                    sql(DatabaseBackupStorageRefVO.class)
                            .eq(DatabaseBackupStorageRefVO_.databaseBackupUuid, msg.getDatabaseBackupUuid())
                            .eq(DatabaseBackupStorageRefVO_.backupStorageUuid, msg.getDstImageStorageUuid())
                            .lt(DatabaseBackupStorageRefVO_.id, r.getId())
                            .delete();
                    return r;
                }

                DatabaseBackupStorageRefVO newRef = new DatabaseBackupStorageRefVO();
                newRef.setBackupStorageUuid(msg.getDstImageStorageUuid());
                newRef.setDatabaseBackupUuid(msg.getNewDatabaseBackupUuid());
                newRef.setInstallPath(installPath);
                newRef.setStatus(DatabaseBackupStatus.Downloading);
                return persist(newRef);
            }
        }.execute();

        if (ref.getStatus().equals(DatabaseBackupStatus.Ready)) {
            completion.success();
            return;
        }

        doPushBits(msg.getSrcImageStorageUuid(),
                msg.getDstImageStorageUuid(),
                msg.getDatabaseBackupUuid(),
                msg.getNewDatabaseBackupUuid(),
                installPath, new Completion(completion) {
                    @Override
                    public void success() {
                        ref.setStatus(DatabaseBackupStatus.Ready);
                        dbf.updateAndRefresh(ref);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        dbf.remove(ref);
                        completion.fail(errorCode);
                    }
                });
    }

    private void doSyncImageBetweenImageStore(final SyncImageBetweenImageStoreMsg msg, Completion completion) {
        String installPath = Q.New(ImageBackupStorageRefVO.class).
                eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getSrcImageStorageUuid()).
                eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid()).select(ImageBackupStorageRefVO_.installPath).findValue();

        ImageBackupStorageRefVO ref = new SQLBatchWithReturn<ImageBackupStorageRefVO>() {
            @Override
            protected ImageBackupStorageRefVO scripts() {
                ImageBackupStorageRefVO r = q(ImageBackupStorageRefVO.class)
                        .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                        .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getDstImageStorageUuid())
                        .orderBy(ImageBackupStorageRefVO_.id, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();
                if (r != null) {
                    sql(ImageBackupStorageRefVO.class)
                            .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                            .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getDstImageStorageUuid())
                            .lt(ImageBackupStorageRefVO_.id, r.getId())
                            .delete();
                    return r;
                }

                ImageBackupStorageRefVO newRef = new ImageBackupStorageRefVO();
                newRef.setBackupStorageUuid(msg.getDstImageStorageUuid());
                newRef.setImageUuid(msg.getNewImageUuid());
                newRef.setInstallPath(installPath);
                newRef.setStatus(ImageStatus.Downloading);
                return persist(newRef);
            }
        }.execute();

        if (ref.getStatus().equals(ImageStatus.Ready)) {
            completion.success();
            return;
        }

        doPushBits(msg.getSrcImageStorageUuid(),
                msg.getDstImageStorageUuid(),
                msg.getImageUuid(),
                msg.getNewImageUuid(),
                installPath, new Completion(completion) {
                    @Override
                    public void success() {
                        ref.setStatus(ImageStatus.Ready);
                        dbf.updateAndRefresh(ref);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        dbf.remove(ref);
                        completion.fail(errorCode);
                    }
                });
    }

    private String getAddressForRemoteSync(String dstImageStoreUuid) {
        String addr = ImageStoreBackupStorage.getSyncNetworkAddress(dstImageStoreUuid);
        if (addr == null) {
            return Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.hostname).
                    eq(ImageStoreBackupStorageVO_.uuid, dstImageStoreUuid).findValue();
        }

        return addr;
    }

    private void handle(final PushBitsBetweenImageStoreMsg msg) {
        PushBitsBetweenImageStoreReply reply = new PushBitsBetweenImageStoreReply();

        String hostName = Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.hostname).
                eq(ImageStoreBackupStorageVO_.uuid, msg.getSrcImageStorageUuid()).findValue();
        String dstHost = getAddressForRemoteSync(msg.getDstImageStorageUuid());

        ImageStoreBackupStorageCommands.PushImageCmd pushCmd = new ImageStoreBackupStorageCommands.PushImageCmd();
        pushCmd.setRefer(msg.getInstallPath());
        pushCmd.setAddr(String.format("%s:%s", dstHost, ImageStoreBackupStorageGlobalProperty.REGISTRY_PORT));
        pushCmd.setCa(getLocalCerts(msg.getSrcImageStorageUuid()));

        ConnectTaskReply task = connectHttps(hostName, ImageStoreBackupStorageGlobalProperty.REGISTRY_PUSH_PATH, JSONObjectUtil.toJsonString(pushCmd), false);
        if (task.isSuccess()) {
            reply.setTaskId(task.getValue());
        } else {
            reply.setError(task.getError());
        }

        bus.reply(msg, reply);
    }

    private void handle(final PullBitsBetweenImageStoreMsg msg) {
        PullBitsBetweenImageStoreReply reply = new PullBitsBetweenImageStoreReply();

        String hostName = Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.hostname).
                eq(ImageStoreBackupStorageVO_.uuid, msg.getSrcImageStorageUuid()).findValue();
        String dstHost = getAddressForRemoteSync(msg.getDstImageStorageUuid());

        ImageStoreBackupStorageCommands.PushImageCmd pullCmd = new ImageStoreBackupStorageCommands.PushImageCmd();
        pullCmd.setRefer(msg.getInstallPath());
        pullCmd.setAddr(String.format("%s:%s", dstHost, ImageStoreBackupStorageGlobalProperty.REGISTRY_PORT));
        pullCmd.setCa(getLocalCerts(msg.getSrcImageStorageUuid()));

        ConnectTaskReply task = connectHttps(hostName, ImageStoreBackupStorageGlobalProperty.REGISTRY_PULL_PATH, JSONObjectUtil.toJsonString(pullCmd), false);
        if (task.isSuccess()) {
            reply.setTaskId(task.getValue());
        } else {
            reply.setError(task.getError());
        }

        bus.reply(msg, reply);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIReclaimSpaceFromImageStoreMsg) {
            handle((APIReclaimSpaceFromImageStoreMsg) msg);
        } else if (msg instanceof APIGetImagesFromImageStoreBackupStorageMsg) {
            handle((APIGetImagesFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISyncImageFromImageStoreBackupStorageMsg) {
            handle((APISyncImageFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISetImageStoreBackupStorageQuotaMsg) {
            handle((APISetImageStoreBackupStorageQuotaMsg) msg);
        } else if (msg instanceof APIRecoveryImageFromImageStoreBackupStorageMsg) {
            handle((APIRecoveryImageFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISyncImageMsg) {
            handle((APISyncImageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIRecoveryImageFromImageStoreBackupStorageMsg msg) {
        APIRecoveryImageFromImageStoreBackupStorageEvent evt = new APIRecoveryImageFromImageStoreBackupStorageEvent(msg.getId());

        ImageVO ivo = dbf.findByUuid(msg.getUuid(), ImageVO.class);
        ivo.setUuid(Platform.getUuid());
        ivo.setName(msg.getName());
        ivo.setDescription(msg.getDescription());
        ivo.setStatus(ImageStatus.Creating);
        ivo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        ivo.setAccountUuid(msg.getSession().getAccountUuid());
        dbf.persistAndRefresh(ivo);

        pluginRgty.getExtensionList(CreateImageExtensionPoint.class).
                forEach(ext -> ext.beforeSyncImage(ImageInventory.valueOf(ivo), msg.getDstBackupStorageUuid()));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("recovery-image-%s-from-src-to-dst-imagestore", msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "recovery image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        RecoveryImageBetweenImageStoreMsg rimsg = new RecoveryImageBetweenImageStoreMsg();
                        rimsg.setDstImageStorageUuid(msg.getDstBackupStorageUuid());
                        rimsg.setSrcImageStorageUuid(msg.getSrcBackupStorageUuid());
                        rimsg.setImageUuid(msg.getUuid());
                        rimsg.setNewImageUuid(ivo.getUuid());

                        bus.makeTargetServiceIdByResourceUuid(rimsg, ImageStoreBackupStorageConstant.SERVICE_ID, msg.getUuid());
                        bus.send(rimsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                RecoveryImageBetweenImageStoreReply reply1 = reply.castReply();
                                if (reply.isSuccess()) {
                                    data.put("taskid", reply1.getTaskId());
                                    createSyncSystemTags(msg.getDstBackupStorageUuid(), msg.getUuid(), ivo.getUuid(), reply1.getTaskId(), SyncTaskStatus.TsWaiting.toString());
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }

                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "wait until success";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String taskId = (String) data.get("taskid");
                        if (taskId.equals(ImageStoreBackupStorageConstant.RESPONSE_TASK_EXISTED_FLAG)) {
                            updateSyncSystemTags(msg.getDstBackupStorageUuid(), msg.getUuid(), ivo.getUuid(), taskId, SyncTaskStatus.TsSuccess.toString());
                            trigger.next();
                        } else {
                            waitUntilReady(taskId, msg.getUuid(), ivo.getUuid(), msg.getDstBackupStorageUuid(), trigger);
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "copy system tag";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        tagMgr.copySystemTag(msg.getUuid(), ImageVO.class.getSimpleName(),
                                ivo.getUuid(), ImageVO.class.getSimpleName());
                        ImageSystemTags.IMAGE_DEPLOY_REMOTE.deleteInherentTag(ivo.getUuid());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        ivo.setStatus(ImageStatus.Ready);
                        dbf.updateAndRefresh(ivo);
                        final ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(ivo.getUuid(), ImageVO.class));
                        evt.setInventory(inv);
                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
                            @Override
                            public void run(AddImageExtensionPoint ext) {
                                ext.afterAddImage(inv);
                            }
                        });

                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        dbf.remove(ivo);
                        deleteSyncSystemTags(msg.getDstBackupStorageUuid(), msg.getUuid());
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void deleteSyncSystemTags(String bsUuid, String imageUuid) {
        ImageStoreSystemTags.SYNC_TASK_STATUS.deleteInherentTag(bsUuid, ImageStoreSystemTags.SYNC_TASK_STATUS.instantiateTag(map(
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_IMAGE_TOKEN, imageUuid)
        )));
    }

    private void createSyncSystemTags(String bsUuid, String imageUuid, String dstUuid, String taskId, String status) {
        SystemTagCreator creator = ImageStoreSystemTags.SYNC_TASK_STATUS.newSystemTagCreator(bsUuid);
        creator.inherent = true;
        creator.recreate = false;
        creator.unique = false;
        creator.setTagByTokens(map(e(ImageStoreSystemTags.IMAGESTORE_SYNC_TASK_TOKEN, taskId),
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_STATUS_TOKEN, status),
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_IMAGE_TOKEN, imageUuid),
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN, dstUuid)));
        creator.create();
    }

    private void updateSyncSystemTags(String bsUuid, String imageUuid, String dstUuid, String taskId, String status) {
        ImageStoreSystemTags.SYNC_TASK_STATUS.updateUnique(bsUuid, ImageStoreSystemTags.SYNC_TASK_STATUS.instantiateTag(map(
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_IMAGE_TOKEN, imageUuid),
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_TASK_TOKEN, taskId),
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN, dstUuid),
                e(ImageStoreSystemTags.IMAGESTORE_SYNC_STATUS_TOKEN, "%"))),

                ImageStoreSystemTags.SYNC_TASK_STATUS.instantiateTag(map(
                        e(ImageStoreSystemTags.IMAGESTORE_SYNC_TASK_TOKEN, taskId),
                        e(ImageStoreSystemTags.IMAGESTORE_SYNC_STATUS_TOKEN, status),
                        e(ImageStoreSystemTags.IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN, dstUuid),
                        e(ImageStoreSystemTags.IMAGESTORE_SYNC_IMAGE_TOKEN, imageUuid)
                )));
    }

    private void chainedSyncImage(SyncImageFromImageStoreBackupStorageMessage msg,
                                  String accountUuid,
                                  ReturnValueCompletion<ImageInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-image-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncImage(msg, accountUuid, new ReturnValueCompletion<ImageInventory>(chain, completion) {
                    @Override
                    public void success(ImageInventory returnValue) {
                        completion.success(returnValue);
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
                return String.format("sync-image-%s-from-bs-%s-to-%s",
                        msg.getUuid(),
                        msg.getSrcBackupStorageUuid(),
                        msg.getDstBackupStorageUuid());
            }
        });
    }

    private void doSyncImage(SyncImageFromImageStoreBackupStorageMessage msg,
                             String accountUuid,
                             ReturnValueCompletion<ImageInventory> completion) {
        ImageVO ivo = dbf.findByUuid(msg.getUuid(), ImageVO.class);
        ivo.setUuid(Platform.getUuid());
        ivo.setName(msg.getName());
        ivo.setDescription(msg.getDescription());
        ivo.setStatus(ImageStatus.Creating);
        ivo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        ivo.setAccountUuid(accountUuid);
        dbf.persistAndRefresh(ivo);

        pluginRgty.getExtensionList(CreateImageExtensionPoint.class).
                forEach(ext -> ext.beforeSyncImage(ImageInventory.valueOf(ivo), msg.getDstBackupStorageUuid()));

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("sync-image-%s-from-src-to-dst-imagestore", msg.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "allocate imagestore capacity";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocateBackupStorageMsg amsg = new AllocateBackupStorageMsg();
                        amsg.setSize(ivo.getActualSize());
                        amsg.setBackupStorageUuid(msg.getDstBackupStorageUuid());
                        bus.makeTargetServiceIdByResourceUuid(amsg, BackupStorageConstant.SERVICE_ID, msg.getDstBackupStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
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
                    String __name__ = "sync image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SyncImageBetweenImageStoreMsg simsg = new SyncImageBetweenImageStoreMsg();
                        simsg.setDstImageStorageUuid(msg.getDstBackupStorageUuid());
                        simsg.setSrcImageStorageUuid(msg.getSrcBackupStorageUuid());
                        simsg.setImageUuid(msg.getUuid());
                        simsg.setNewImageUuid(ivo.getUuid());

                        bus.makeTargetServiceIdByResourceUuid(simsg, ImageStoreBackupStorageConstant.SERVICE_ID, msg.getUuid());
                        bus.send(simsg, new CloudBusCallBack(trigger) {
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
                    String __name__ = "copy system tag";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        tagMgr.copySystemTag(msg.getUuid(), ImageVO.class.getSimpleName(),
                                ivo.getUuid(), ImageVO.class.getSimpleName());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        ivo.setStatus(ImageStatus.Ready);
                        dbf.updateAndRefresh(ivo);
                        final ImageInventory inv = ImageInventory.valueOf(dbf.findByUuid(ivo.getUuid(), ImageVO.class));

                        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AddImageExtensionPoint.class), new ForEachFunction<AddImageExtensionPoint>() {
                            @Override
                            public void run(AddImageExtensionPoint ext) {
                                ext.afterAddImage(inv);
                            }
                        });

                        completion.success(inv);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        dbf.remove(ivo);
                        deleteSyncSystemTags(msg.getSrcBackupStorageUuid(), msg.getUuid());
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final SyncImageFromImageStoreBackupStorageMsg msg) {
        final String accountUuid = acntMgr.getOwnerAccountUuidOfResource(msg.getUuid());
        chainedSyncImage(msg, accountUuid, new ReturnValueCompletion<ImageInventory>(msg) {
            SyncImageFromImageStoreBackupStorageReply reply = new SyncImageFromImageStoreBackupStorageReply();

            @Override
            public void success(ImageInventory returnValue) {
                reply.setInventory(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final APISetImageStoreBackupStorageQuotaMsg msg) {
        APISetImageStoreBackupStorageQuotaEvent event = new APISetImageStoreBackupStorageQuotaEvent(msg.getId());
        List<String> bsUuids = msg.getUuids();
        ResourceConfig rconf = rcf.getResourceConfig(ImageStoreGlobalConfig.MAX_CAPACITY.getIdentity());
        boolean updateResourceConfig = bsUuids != null && !bsUuids.isEmpty();
        if (!updateResourceConfig) {
            bsUuids = Q.New(ImageStoreBackupStorageVO.class).select(ImageStoreBackupStorageVO_.uuid).listValues();
            bsUuids.forEach(rconf::deleteValue);
            ImageStoreGlobalConfig.MAX_CAPACITY.updateValue(msg.getMaxCapacity());
        }

        ErrorCodeList err = new ErrorCodeList();
        new While<>(bsUuids).each((bsUuid, compl) -> {
            SetImageStoreQuotaMsg smsg = new SetImageStoreQuotaMsg();
            smsg.setQuota(msg.getMaxCapacity());
            smsg.setUuid(bsUuid);
            bus.makeTargetServiceIdByResourceUuid(smsg, BackupStorageConstant.SERVICE_ID, bsUuid);
            bus.send(smsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        err.getCauses().add(reply.getError());
                        compl.allDone();
                        return;
                    }

                    if (updateResourceConfig) {
                        rconf.updateValue(bsUuid, String.valueOf(smsg.getQuota()));
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!err.getCauses().isEmpty()) {
                    event.setError(err.getCauses().get(0));
                }
                bus.publish(event);
            }
        });
    }

    private void handle(final APISyncImageFromImageStoreBackupStorageMsg msg) {
        chainedSyncImage(msg, msg.getSession().getAccountUuid(), new ReturnValueCompletion<ImageInventory>(msg) {
            APISyncImageFromImageStoreBackupStorageEvent evt = new APISyncImageFromImageStoreBackupStorageEvent(msg.getId());

            @Override
            public void success(ImageInventory returnValue) {
                evt.setInventory(returnValue);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(final APIGetImagesFromImageStoreBackupStorageMsg msg) {
        APIGetImagesFromImageStoreBackupStorageReply reply = new APIGetImagesFromImageStoreBackupStorageReply();
        ListImagesFromImageStoreMsg lmsg = new ListImagesFromImageStoreMsg();
        lmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(lmsg, BackupStorageConstant.SERVICE_ID, lmsg.getUuid());
        bus.send(lmsg, new CloudBusCallBack(lmsg) {
            @Override
            public void run(MessageReply reply1) {
                if (!reply1.isSuccess()) {
                    reply.setError(reply1.getError());
                } else {
                    ListImagesFromImageStoreReply rly = reply1.castReply();
                    reply.setInfos(rly.getStructs());
                }
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "deploy_zstack_imagestore_client";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    trigger.next();
                    return;
                }

                if (Q.New(ImageStoreBackupStorageVO.class)
                        .eq(ImageStoreBackupStorageVO_.hostname, context.getInventory().getManagementIp())
                        .isExists()) {
                    logger.warn("skip imagestore client deployment on kvm-host, it is also zstore.");
                    trigger.next();
                    return;
                }

                SshFileMd5Checker checker = new SshFileMd5Checker();
                KVMHostInventory inv = context.getInventory();
                String hostname = inv.getManagementIp();
                Integer port = inv.getSshPort();
                String username = inv.getUsername();
                String password = inv.getPassword();

                checker.setTargetIp(hostname);
                checker.setUsername(username);
                checker.setPassword(password);
                checker.setSshPort(port);
                String srcAgentName = agentPackageName;
                if (inv.getArchitecture() != null && !HostConstant.HOST_ARCHITECTURE_X86_64.equals(inv.getArchitecture())) {
                    srcAgentName = srcAgentName.replace("bin", inv.getArchitecture()+ ".bin");
                }
                checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/imagestorebackupstorage/%s", srcAgentName), true).getAbsolutePath(),
                        String.format("/var/lib/zstack/imagestorebackupstorage/package/%s", agentClientPackageName));

                SshFileMd5Checker caChecker = new SshFileMd5Checker();
                caChecker.setTargetIp(hostname);
                caChecker.setUsername(username);
                caChecker.setPassword(password);
                caChecker.setSshPort(port);
                caChecker.addSrcDestPair((PathUtil.join(PathUtil.getZStackHomeFolder(), "imagestore", "bin") + "/certs/ca.pem"),
                        ImageStoreBackupStorageGlobalProperty.REGISTRY_CERTS);

                CallBackNetworkChecker callbackChecker = new CallBackNetworkChecker();
                callbackChecker.setTargetIp(hostname);
                callbackChecker.setUsername(username);
                callbackChecker.setPassword(password);
                callbackChecker.setPort(port);
                callbackChecker.setCallbackIp(Platform.getManagementServerIp());
                callbackChecker.setCallBackPort(CloudBusGlobalProperty.HTTP_PORT);

                AnsibleRunner runner = new AnsibleRunner();
                runner.installChecker(checker);
                runner.installChecker(caChecker);
                runner.installChecker(callbackChecker);
                runner.setPassword(password);
                runner.setUsername(username);
                runner.setTargetIp(hostname);
                runner.setTargetUuid(inv.getUuid());
                runner.setSshPort(port);
                runner.setPlayBookName(ImageStoreBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);

                ImageStoreAgentDeployArguments deployArguments = new ImageStoreAgentDeployArguments();
                deployArguments.setClient("true");
                deployArguments.setSkipPackages(context.getSkipPackages());
                runner.setDeployArguments(deployArguments);
                runner.run(new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean deployed) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    private void setupDisasterGC() {
        String gcName = ImageStoreDisasterGC.getGCName();

        boolean hasGC = Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.name, gcName)
                .eq(GarbageCollectorVO_.type, GarbageCollectorType.CycleBased.toString())
                .notEq(GarbageCollectorVO_.status, GCStatus.Done)
                .isExists();

        if (hasGC) {
            // Garbage collector  manager will load GC
            return;
        }

        // refresh gc cycle time
        ImageStoreDisasterGC gc = new ImageStoreDisasterGC();
        gc.NAME = gcName;
        gc.submit(ImageStoreBackupStorageConstant.RESPONSE_TASK_RUN_INTERVAL, TimeUnit.SECONDS);
    }

    private void deleteCurrentImageStoreCA(String imageStoreBinDir) {
        File currentCaFile = new File(imageStoreBinDir + "/certs/ca.pem");
        if (!currentCaFile.exists() || currentCaFile.delete()) {
            SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, "imageStoreCA").delete();
        } else {
            logger.warn(String.format("file %s exists, but delete failed", currentCaFile.getName()));
        }

        File currentPrivateKeyFile = new File(imageStoreBinDir + "/certs/privkey.pem");
        if (!currentPrivateKeyFile.exists() || currentPrivateKeyFile.delete()) {
            SQL.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, "imageStorePrivateKey").delete();
        } else {
            logger.warn(String.format("file %s exists, but delete failed", currentPrivateKeyFile.getName()));
        }
    }

    private boolean noCertFile(String imageStoreBinDir) {
        File caFile = new File(imageStoreBinDir + "/certs/ca.pem");
        return !caFile.exists();
    }

    private boolean certSubjectChanged(boolean noCertFile) {
        if (noCertFile) {
            return false;
        }

        try {
            String currentCA = new JsonLabel().get("imageStoreCA", String.class);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(currentCA.getBytes()));

            if (x509Cert.getSubjectAlternativeNames() == null) {
                return true;
            } else {
                boolean noMatched = x509Cert
                        .getSubjectAlternativeNames()
                        .stream()
                        .noneMatch(san -> san.contains("store.zstack.org"));
                if (noMatched) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("failed to check CA cert", e);
        }
        return false;
    }

    @Override
    public boolean start() {
        installValidatorToSystemTag();
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        // Generate certificates regardless of license type.  Otherwise, user
        // need to restart the management node to generate these certificates.
        String imageStoreBinDir = PathUtil.join(PathUtil.getZStackHomeFolder(), "imagestore", "bin");

        try {
            File dst = new File(imageStoreBinDir);
            if (!dst.exists()) {
                dst.mkdirs();
            }

            if (certSubjectChanged(noCertFile(imageStoreBinDir))) {
                deleteCurrentImageStoreCA(imageStoreBinDir);
            }

            File caFile = new File(imageStoreBinDir + "/certs/ca.pem");
            if (!caFile.exists()) {
                File scriptPath = PathUtil.findFileOnClassPath("scripts/generate-keys.sh");
                ShellUtils.run("bash " + scriptPath.getAbsolutePath(), imageStoreBinDir, false);
                caFile = new File(imageStoreBinDir + "/certs/ca.pem");
            }

            String ca = FileUtils.readFileToString(caFile);
            ca = ca.trim();
            ca = StringDSL.stripEnd(ca, "\n");
            File privateKeyFile = new File(imageStoreBinDir + "/certs/privkey.pem");
            String privateKey = FileUtils.readFileToString(privateKeyFile);
            privateKey = privateKey.trim();
            privateKey = StringDSL.stripEnd(privateKey, "\n");

            JsonLabelInventory caInventory = new JsonLabel().createIfAbsent("imageStoreCA", ca);
            JsonLabelInventory privateKeyInventory = new JsonLabel().createIfAbsent("imageStorePrivateKey", privateKey);

            //old version didn't generate key in database, we will write back key for HA environment
            FileUtils.writeStringToFile(caFile, caInventory.getLabelValue());
            ShellUtils.run(String.format("chmod 600 %s", privateKeyFile.getAbsolutePath()));
            FileUtils.writeStringToFile(privateKeyFile, privateKeyInventory.getLabelValue());
            ShellUtils.run(String.format("chmod 400 %s", privateKeyFile.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            logger.warn("failed to create directory: " + imageStoreBinDir, e);
            return false;
        }
    }

    private void installValidatorToSystemTag() {
        ImageStoreSystemTags.SYNC_NETWORK.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String cidr = ImageStoreSystemTags.SYNC_NETWORK.getTokenByTag(systemTag,
                        ImageStoreSystemTags.SYNC_NETWORK_TOKEN);
                String fmtCidr = NetworkUtils.fmtCidr(cidr);
                if (!fmtCidr.equals(cidr)) {
                    throw new OperationFailureException(argerr("[%s] is not a standard cidr, do you mean [%s]?", cidr, fmtCidr));
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {
        try {
            setupDisasterGC();
        } catch (Exception e) {
            logger.warn("setup disaster recovery GC", e);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ImageStoreBackupStorageConstant.SERVICE_ID);
    }

    private void handle(APIReclaimSpaceFromImageStoreMsg msg) {
        APIReclaimSpaceFromImageStoreEvent evt = new APIReclaimSpaceFromImageStoreEvent(msg.getId());
        ReclaimSpaceFromImageStoreMsg rmsg = new ReclaimSpaceFromImageStoreMsg();
        rmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, rmsg.getUuid());
        bus.send(rmsg, new CloudBusCallBack(rmsg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    ReclaimSpaceFromImageStoreReply r = reply.castReply();
                    ImageStoreGcResult res = new ImageStoreGcResult();
                    res.setFreedSpaceInBytes(r.getFreedSpaceInBytes());
                    evt.setGcResult(res);
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APISyncImageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-imagestore-%s-image-from-metadata", msg.getImageStoreUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APISyncImageEvent event = new APISyncImageEvent(msg.getId());

                ImageStoreBackupStorageVO vo = Q.New(ImageStoreBackupStorageVO.class)
                        .eq(ImageStoreBackupStorageVO_.uuid, msg.getImageStoreUuid()).find();
                metaDataMaker.restoreImagesBackupStorageMetadataToDatabase(ImageStoreBackupStorageInventory.valueOf(vo), new Completion(chain) {
                    @Override
                    public void success() {
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
                return getSyncSignature();
            }
        });
    }
}
