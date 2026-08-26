package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.*;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.storage.ceph.*;
import org.zstack.storage.ceph.primary.capacity.CephOsdGroupCapacityHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageMonBase extends CephMonBase {
    private static final CLogger logger = Utils.getLogger(CephPrimaryStorageMonBase.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private String syncId;

    public static final String ECHO_PATH = "/ceph/primarystorage/echo";
    public static final String PING_PATH = "/ceph/primarystorage/ping";

    public enum PingOperationFailure {
        UnableToCreateFile,
        MonAddrChanged,
    }


    public static class AgentCmd {
        public String monUuid;
        public String primaryStorageUuid;
    }

    public static class AgentRsp extends CephPrimaryStorageBase.AgentResponse {
    }

    public static class PingCmd extends AgentCmd {
        public String testImagePath;
        public String monAddr;
    }

    public static class PingRsp extends AgentRsp {
        public boolean operationFailure;
        public PingOperationFailure failure;
    }

    public CephPrimaryStorageMonVO getSelf() {
        return (CephPrimaryStorageMonVO) self;
    }

    public void changeStatus(MonStatus status) {
        String uuid = self.getUuid();
        self = dbf.reload(self);
        if (self == null) {
            throw new OperationFailureException(operr(
                    "cannot update status of the ceph primary storage mon[uuid:%s], it has been deleted." +
                            "This error can be ignored", uuid
            ));
        }

        if (self.getStatus() == status) {
            return;
        }

        MonStatus oldStatus = self.getStatus();
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("Ceph primary storage mon[uuid:%s] changed status from %s to %s", self.getUuid(), oldStatus, status));
    }

    @Override
    public void connect(final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                doConnect(new Completion(completion, chain) {
                    @Override
                    public void success() {
                        completion.success();
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
                return String.format("connect-ceph-primary-storage-mon-%s", self.getUuid());
            }
        });
    }

    private void doConnect(final Completion completion) {
        changeStatus(MonStatus.Connecting);

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-mon-%s-ceph-primary-storage-%s", self.getHostname(), getSelf().getPrimaryStorageUuid()));
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (!CoreGlobalProperty.UNIT_TEST_ON) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "check-tools";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            checkTools();
                            checkHealth();
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "deploy-agent";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            SshFileMd5Checker checker = new SshFileMd5Checker();
                            checker.setTargetIp(getSelf().getHostname());
                            checker.setUsername(getSelf().getSshUsername());
                            checker.setPassword(getSelf().getSshPassword());
                            checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/cephp/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/cephp/%s", CephGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME), true).getAbsolutePath(),
                                    String.format("/var/lib/zstack/cephp/package/%s", CephGlobalProperty.PRIMARY_STORAGE_PACKAGE_NAME));

                            SshChronyConfigChecker chronyChecker = new SshChronyConfigChecker();
                            chronyChecker.setTargetIp(getSelf().getHostname());
                            chronyChecker.setUsername(getSelf().getSshUsername());
                            chronyChecker.setPassword(getSelf().getSshPassword());
                            chronyChecker.setSshPort(getSelf().getSshPort());

                            SshYumRepoChecker repoChecker = new SshYumRepoChecker();
                            repoChecker.setTargetIp(self.getHostname());
                            repoChecker.setUsername(self.getSshUsername());
                            repoChecker.setPassword(self.getSshPassword());
                            repoChecker.setSshPort(self.getSshPort());

                            CallBackNetworkChecker callbackChecker = new CallBackNetworkChecker();
                            callbackChecker.setTargetIp(getSelf().getHostname());
                            callbackChecker.setUsername(getSelf().getSshUsername());
                            callbackChecker.setPassword(getSelf().getSshPassword());
                            callbackChecker.setPort(getSelf().getSshPort());
                            callbackChecker.setCallbackIp(Platform.getManagementServerIp());
                            callbackChecker.setCallBackPort(CloudBusGlobalProperty.HTTP_PORT);

                            AnsibleRunner runner = new AnsibleRunner();
                            runner.installChecker(checker);
                            runner.installChecker(chronyChecker);
                            runner.installChecker(repoChecker);
                            runner.installChecker(callbackChecker);
                            runner.setPassword(getSelf().getSshPassword());
                            runner.setUsername(getSelf().getSshUsername());
                            runner.setSshPort(getSelf().getSshPort());
                            runner.setTargetIp(getSelf().getHostname());
                            runner.setTargetUuid(getSelf().getUuid());
                            runner.setAgentPort(CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT);
                            runner.setPlayBookName(CephGlobalProperty.PRIMARY_STORAGE_PLAYBOOK_NAME);

                            CephPrimaryStorageDeployArguments deployArguments = new CephPrimaryStorageDeployArguments();
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
                    });

                }

                flow(new NoRollbackFlow() {
                    String __name__ = "deploy-more-agent-to-primaryStorage";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<CephMonExtensionPoint> exts = pluginRgty.getExtensionList(CephMonExtensionPoint.class);
                        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                        chain.allowEmptyFlow();
                        for(CephMonExtensionPoint ext: exts) {
                            chain.then(new NoRollbackFlow() {
                                @Override
                                public void run(FlowTrigger trigger1, Map data) {
                                    ext.addMoreAgentInPrimaryStorage(CephPrimaryStorageMonInventory.valueOf(getSelf()), new Completion(trigger1) {
                                        @Override
                                        public void success() {
                                            trigger1.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger1.fail(errorCode);
                                        }
                                    });
                                }
                            });
                        }
                        chain.done(new FlowDoneHandler(trigger) {
                            @Override
                            public void handle(Map data) {
                                trigger.next();
                            }
                        }).error(new FlowErrorHandler(trigger) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                trigger.fail(errCode);
                            }
                        }).start();
                    }
                });

                if (!CoreGlobalProperty.UNIT_TEST_ON) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "configure-iptables";

                        @Autowired
                        public void run(FlowTrigger trigger, Map data) {
                            StringBuilder builder = new StringBuilder();
                            if (!CephGlobalProperty.MN_NETWORKS.isEmpty()) {
                                builder.append(String.format("sudo bash %s -m %s -p %s -c %s",
                                        "/var/lib/zstack/cephp/package/cephps-iptables",
                                        CephConstants.CEPH_PS_IPTABLES_COMMENTS,
                                        CephGlobalConfig.CEPH_PS_ALLOW_PORTS.value(String.class),
                                        String.join(",", CephGlobalProperty.MN_NETWORKS)));
                            } else {
                                builder.append(String.format("sudo bash %s -m %s -p %s",
                                        "/var/lib/zstack/cephp/package/cephps-iptables",
                                        CephConstants.CEPH_PS_IPTABLES_COMMENTS,
                                        CephGlobalConfig.CEPH_PS_ALLOW_PORTS.value(String.class)));
                            }

                            try {
                                new Ssh().shell(builder.toString())
                                        .setUsername(self.getSshUsername())
                                        .setPassword(self.getSshPassword())
                                        .setHostname(self.getHostname())
                                        .setPort(self.getSshPort()).runErrorByExceptionAndClose();
                            } catch (SshException ex) {
                                throw new OperationFailureException(operr(ex.toString()));
                            }

                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "echo-agent";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        restf.echo(CephAgentUrl.primaryStorageUrl(getSelf().getHostname(), ECHO_PATH), new Completion(trigger) {
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

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        changeStatus(MonStatus.Connected);
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        changeStatus(MonStatus.Disconnected);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void httpCall(String path, AgentCmd cmd, final ReturnValueCompletion<AgentRsp> completion) {
        httpCall(path, cmd, AgentRsp.class, completion);
    }

    private <T extends AgentResponse> void httpCall(String path, AgentCmd cmd, final Class<T> rspClass, final ReturnValueCompletion<T> completion) {
        restf.asyncJsonPost(CephAgentUrl.primaryStorageUrl(self.getHostname(), path),
                cmd, new JsonAsyncRESTCallback<T>(completion) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(T ret) {
                        if (!ret.isSuccess()) {
                            completion.fail(Platform.operr("operation error, because:%s", ret.getError()));
                            return;
                        }
                        completion.success(ret);
                    }

                    @Override
                    public Class<T> getReturnClass() {
                        return rspClass;
                    }
                });
    }

    @Override
    public void ping(final ReturnValueCompletion<PingResult> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return syncId;
            }

            @Override
            public void run(final SyncTaskChain chain) {
                pingMon(new ReturnValueCompletion<PingResult>(completion) {
                    @Override
                    public void success(PingResult ret) {
                        completion.success(ret);
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
                return String.format("ping-ceph-primary-storage-%s", self.getUuid());
            }
        });
    }

    @Override
    protected int getAgentPort() {
        return CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT;
    }

    @Override
    protected String makeHttpPath(String ip, String path) {
        return CephAgentUrl.primaryStorageUrl(ip, path);
    }

    private void pingMon(final ReturnValueCompletion<PingResult> completion) {
        final Integer MAX_PING_CNT = CephGlobalConfig.PRIMARY_STORAGE_MON_MAXIMUM_PING_FAILURE.value(Integer.class);
        final List<Integer> stepCount = new ArrayList<>();
        for (int i = 1; i <= MAX_PING_CNT; i++) {
            stepCount.add(i);
        }

        PingResult pingResult = new PingResult();
        new While<>(stepCount).each((step, compl) -> {
            doPing(new ReturnValueCompletion<PingResult>(completion) {
                @Override
                public void success(PingResult returnValue) {
                    pingResult.success = returnValue.success;
                    pingResult.error = returnValue.error;
                    pingResult.failure = returnValue.failure;
                    compl.allDone();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("ping ceph ps mon[%s] failed (%d/%d):%n%s",
                            self.getMonAddr(), step, MAX_PING_CNT, errorCode.getReadableDetails()));
                    compl.addError(errorCode);

                    if (step.equals(MAX_PING_CNT)) {
                        compl.allDone();
                        return;
                    }

                    int sleep = CephGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.value(Integer.class);
                    if (sleep > 0) {
                        try {
                            TimeUnit.SECONDS.sleep(sleep);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().size() == MAX_PING_CNT) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                completion.success(pingResult);
            }
        });
    }

    private void doPing(final ReturnValueCompletion<PingResult> completion) {
        String primaryStorageUuid = Q.New(CephPrimaryStorageMonVO.class)
                .select(CephPrimaryStorageMonVO_.primaryStorageUuid)
                .eq(CephPrimaryStorageMonVO_.uuid, self.getUuid())
                .findValue();
        String poolName = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.poolName)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.type, CephPrimaryStoragePoolType.Root.toString())
                .limit(1)
                .findValue();
        if (poolName == null) {
            completion.fail(operr("Ceph ps[uuid=%s] root pool name not found", primaryStorageUuid));
            return;
        }

        PingCmd cmd = new PingCmd();
        cmd.testImagePath = String.format("%s/zshb.ps.%s.%s", poolName, self.getUuid(), self.getMonAddr());
        cmd.monUuid = getSelf().getUuid();
        cmd.primaryStorageUuid = getSelf().getPrimaryStorageUuid();
        cmd.monAddr = String.format("%s:%s", getSelf().getMonAddr(), getSelf().getMonPort());

        restf.asyncJsonPost(CephAgentUrl.primaryStorageUrl(self.getHostname(), PING_PATH),
                cmd, new JsonAsyncRESTCallback<PingRsp>(completion) {
                    @Override
                    public void fail(ErrorCode err) {
                        completion.fail(err);
                    }

                    @Override
                    public void success(PingRsp rsp) {
                        PingResult res = new PingResult();
                        res.success = rsp.isSuccess();
                        res.error = rsp.getError();
                        // if agent met unexpected error, no failure will be set
                        res.failure = Objects.toString(rsp.failure, null);
                        completion.success(res);

                        if (rsp.isSuccess() && rsp.availableCapacity != null && rsp.totalCapacity != null) {
                            String fsid = Q.New(CephPrimaryStorageVO.class)
                                    .select(CephPrimaryStorageVO_.fsid)
                                    .eq(CephPrimaryStorageVO_.uuid, primaryStorageUuid)
                                    .findValue();
                            CephCapacity cephCapacity = new CephCapacity(fsid, rsp);
                            new CephCapacityUpdater().update(cephCapacity);
                            new CephOsdGroupCapacityHelper(primaryStorageUuid).recalculateAvailableCapacity();
                        }
                    }

                    @Override
                    public Class<PingRsp> getReturnClass() {
                        return PingRsp.class;
                    }
                }, TimeUnit.SECONDS, 60);
    }

    public CephPrimaryStorageMonBase(CephMonAO self) {
        super(self);
        syncId = String.format("ceph-primary-storage-mon-%s", self.getUuid());
    }
}
