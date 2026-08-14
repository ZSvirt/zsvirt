package org.zstack.console;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleConstant;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.console.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.*;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManagementServerConsoleProxyBackend extends AbstractConsoleProxyBackend {
    private static final CLogger logger = Utils.getLogger(ManagementServerConsoleProxyBackend.class);
    private int agentPort = 7758;
    private String agentPackageName = ConsoleGlobalProperty.AGENT_PACKAGE_NAME;
    private boolean connected = false;

    public static ConsoleProxyAgentType type = new ConsoleProxyAgentType(ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_TYPE);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ConsoleProxyAgentTracker tracker;

    protected int setConsoleProxyOverridenIp(String newIp) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return 0;
        }

        ShellResult rst = ShellUtils.runAndReturn(
                "/usr/bin/zstack-ctl configure consoleProxyOverriddenIp=" + newIp
        );
        return rst.getRetCode();
    }

    protected int setConsoleProxyPort(int Port) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return 0;
        }

        ShellResult rst = ShellUtils.runAndReturn(
                "/usr/bin/zstack-ctl configure consoleProxyPort=" + Port
        );
        return rst.getRetCode();
    }

    protected ConsoleProxy getConsoleProxy(VmInstanceInventory vm, ConsoleProxyVO vo) {
        return new ConsoleProxyBase(vo, getAgentPort());
    }

    @Override
    protected ConsoleProxy getConsoleProxy(SessionInventory session, VmInstanceInventory vm) {
        String mgmtIp = CoreGlobalProperty.UNIT_TEST_ON ? "127.0.0.1" : Platform.getManagementServerIp();
        ConsoleProxyInventory inv = new ConsoleProxyInventory();
        inv.setScheme("http");
        inv.setProxyHostname(mgmtIp);
        inv.setAgentIp("127.0.0.1");
        inv.setAgentType(getConsoleBackendType());
        inv.setToken(session.getUuid() + "_" + vm.getUuid());
        inv.setVmInstanceUuid(vm.getUuid());
        return new ConsoleProxyBase(inv, getAgentPort());
    }

    @Override
    protected ConsoleProxy getConsoleProxy(ConsoleProxyInventory proxy) {
        return new ConsoleProxyBase(proxy, getAgentPort());
    }

    private void setupPublicKey() {
        File pubKeyFile = PathUtil.findFileOnClassPath(AnsibleConstant.RSA_PUBLIC_KEY);
        String script = PathUtil.findFileOnClassPath(AnsibleConstant.IMPORT_PUBLIC_KEY_SCRIPT_PATH, true).getAbsolutePath();

        if (pubKeyFile != null) {
            ShellUtils.run(String.format("sh %s '%s'", script, pubKeyFile.getAbsolutePath()));
        }
    }

    protected void doConnectAgent(final Completion completion) {
        doConnectAgent(false, completion);
    }

    protected void doConnectAgent(boolean fullDeploy, final Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("deploy-console-agent-%s", Platform.getManagementServerId());
            }

            @Override
            @Deferred
            public void run(final SyncTaskChain chain) {
                ConsoleProxyAgentVO vo = dbf.findByUuid(Platform.getManagementServerId(), ConsoleProxyAgentVO.class);
                if (vo == null) {
                    vo = new ConsoleProxyAgentVO();
                    vo.setManagementIp(Platform.getManagementServerIp());
                    vo.setUuid(Platform.getManagementServerId());
                    vo.setConsoleProxyOverriddenIp(CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP);
                    vo.setConsoleProxyPort(CoreGlobalProperty.CONSOLE_PROXY_PORT);
                    vo.setState(ConsoleProxyAgentState.Enabled);
                    vo.setStatus(ConsoleProxyAgentStatus.Connecting);
                    vo.setDescription(String.format("Console proxy agent running on the management node[uuid:%s]", Platform.getManagementServerId()));
                    vo.setType(ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_TYPE);
                    vo = dbf.persistAndRefresh(vo);
                }

                final ConsoleProxyAgentVO finalVo = vo;
                Defer.guard(new Runnable() {
                    @Override
                    public void run() {
                        finalVo.setStatus(ConsoleProxyAgentStatus.Disconnected);
                        dbf.update(finalVo);
                    }
                });

                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    finalVo.setStatus(ConsoleProxyAgentStatus.Connected);
                    dbf.update(finalVo);

                    connected = true;
                    logger.debug("successfully deploy console proxy agent by ansible");
                    completion.success();
                    chain.next();
                    return;
                }

                try {
                    ShellUtils.run("rm -rf /var/lib/zstack/consoleProxy/ && mkdir -p /var/lib/zstack/consoleProxy/");
                    StringBuilder builder = new StringBuilder();
                    String[] ports = ConsoleGlobalConfig.VNC_ALLOW_PORTS_LIST.value(String.class).split(",");
                    if (!ConsoleGlobalProperty.MN_NETWORKS.isEmpty()) {
                        builder.append(String.format("sudo ipset create ZS-MN hash:net -exist; sudo ipset flush ZS-MN"));
                        for (String net : ConsoleGlobalProperty.MN_NETWORKS) {
                            builder.append(String.format(";sudo ipset add ZS-MN %s", net));
                        }

                        builder.append(String.format(";sudo iptables-save | grep '%s' | while read LINE;do drule=${LINE//\\\"/};sudo iptables -w ${drule/-A/-D};done",
                                ConsoleConstants.VNC_IPTABLES_COMMENTS));
                        for (String dport : ports) {
                            builder.append(String.format(";sudo iptables -w -I INPUT -m set --match-set ZS-MN src -p tcp -m comment --comment %s -m tcp --dport %s -j ACCEPT",
                                    ConsoleConstants.VNC_IPTABLES_COMMENTS, dport));
                        }
                        builder.append(String.format(";sudo iptables -w -I INPUT -m set --match-set ZS-MN src -p tcp -m comment --comment %s -m tcp ! -s 127.0.0.1 --dport %s -j REJECT",
                                ConsoleConstants.VNC_IPTABLES_COMMENTS, agentPort));
                    } else {
                        builder.append(String.format("sudo iptables-save | grep '%s' | while read LINE;do drule=${LINE//\\\"/};sudo iptables -w ${drule/-A/-D};done",
                                ConsoleConstants.VNC_IPTABLES_COMMENTS));
                        for (String dport : ports) {
                            builder.append(String.format(";sudo iptables -w -I INPUT -p tcp -m comment --comment %s -m tcp --dport %s -j ACCEPT",
                                    ConsoleConstants.VNC_IPTABLES_COMMENTS, dport));
                        }
                        builder.append(String.format(";sudo iptables -w -I INPUT -p tcp -m comment --comment %s -m tcp ! -s 127.0.0.1 --dport %s -j REJECT",
                                ConsoleConstants.VNC_IPTABLES_COMMENTS, agentPort));
                    }
                    ShellUtils.run(builder.toString());

                    setupPublicKey();
                    File privKeyFile = PathUtil.findFileOnClassPath("ansible/rsaKeys/id_rsa");
                    if (privKeyFile == null) {
                        completion.fail(operr("Ansible private key not found."));
                        chain.next();
                        return;
                    }

                    String privKey = FileUtils.readFileToString(privKeyFile);

                    String srcPath = PathUtil.findFileOnClassPath(String.format("ansible/consoleproxy/%s", agentPackageName), true).getAbsolutePath();
                    String destPath = String.format("/var/lib/zstack/console/package/%s", agentPackageName);
                    SshFileMd5Checker checker = new SshFileMd5Checker();
                    checker.setTargetIp("127.0.0.1");
                    checker.setUsername("root");
                    checker.setPrivateKey(privKey);
                    checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/console/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
                    checker.addSrcDestPair(srcPath, destPath);

                    AnsibleRunner runner = new AnsibleRunner();
                    runner.setRunOnLocal(true);
                    runner.setLocalPublicKey(true);
                    runner.installChecker(checker);
                    runner.setUsername("root");
                    runner.setPrivateKey(privKey);
                    runner.setAgentPort(7758);
                    runner.setTargetIp(Platform.getManagementServerIp());
                    runner.setTargetUuid(Platform.getManagementServerId());
                    runner.setPlayBookName(ANSIBLE_PLAYBOOK_NAME);
                    runner.setFullDeploy(fullDeploy);

                    ConsoleProxyDeployArguments deployArguments = new ConsoleProxyDeployArguments();
                    deployArguments.setHttpConsoleProxyPort(String.valueOf(CoreGlobalProperty.HTTP_CONSOLE_PROXY_PORT));
                    runner.setDeployArguments(deployArguments);
                    runner.run(new ReturnValueCompletion<Boolean>(completion, chain) {
                        @Override
                        public void success(Boolean deployed) {
                            finalVo.setStatus(ConsoleProxyAgentStatus.Connected);
                            dbf.update(finalVo);

                            connected = true;
                            logger.debug("successfully deploy console proxy agent by ansible");
                            completion.success();
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            finalVo.setStatus(ConsoleProxyAgentStatus.Disconnected);
                            dbf.update(finalVo);

                            connected = false;
                            logger.warn(String.format("failed to deploy console proxy agent by ansible, %s", errorCode.getDetails()));
                            completion.fail(errorCode);
                            chain.next();
                        }
                    });
                } catch (IOException e) {
                    throw new CloudRuntimeException(e);
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    @AsyncThread
    protected void connectAgent() {
        doConnectAgent(new NopeCompletion());
    }

    @Override
    protected boolean isAgentConnected() {
        return connected;
    }

    @Override
    public String getConsoleBackendType() {
        return ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_BACKEND;
    }

    @Override
    public void deleteConsoleSession(ConsoleProxyInventory consoleProxy, Completion completion) {
        ConsoleProxy proxy = getConsoleProxy(consoleProxy);
        proxy.deleteProxy(consoleProxy, completion);
    }

    @Override
    public String returnServiceIdForConsoleAgentMsg(ConsoleProxyAgentMessage msg, String agentUuid) {
        return bus.makeServiceIdByManagementNodeId(ConsoleConstants.SERVICE_ID, Platform.getManagementServerId());
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }

    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ReconnectConsoleProxyMsg) {
            handle((ReconnectConsoleProxyMsg) msg);
        } else if (msg instanceof PingConsoleProxyAgentMsg) {
            handle((PingConsoleProxyAgentMsg) msg);
        } else if (msg instanceof UpdateConsoleProxyAgentMsg) {
            handle((UpdateConsoleProxyAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(PingConsoleProxyAgentMsg msg) {
        ConsoleProxyCommands.PingCmd cmd = new ConsoleProxyCommands.PingCmd();
        String url = URLBuilder.buildHttpUrl("127.0.0.1", agentPort, ConsoleConstants.CONSOLE_PROXY_PING_PATH);
        ConsoleProxyAgentVO vo = dbf.findByUuid(Platform.getManagementServerId(), ConsoleProxyAgentVO.class);

        boolean success;
        boolean reconnect = false;
        try {
            restf.syncJsonPost(url, cmd, ConsoleProxyCommands.PingRsp.class);
            success = true;
            if (vo != null) {
                reconnect = vo.getStatus() == ConsoleProxyAgentStatus.Disconnected;

                if (vo.getStatus() != ConsoleProxyAgentStatus.Connected) {
                    vo.setStatus(ConsoleProxyAgentStatus.Connected);
                    dbf.update(vo);
                }
            } else {
                reconnect = true;
            }

        } catch (Exception e) {
            logger.warn(String.format("cannot ping console proxy agent, %s", e.getMessage()), e);

            if (vo != null) {
                vo.setStatus(ConsoleProxyAgentStatus.Disconnected);
                dbf.update(vo);
            }
            success = false;
        }

        PingConsoleProxyAgentReply reply = new PingConsoleProxyAgentReply();
        reply.setConnected(success);
        reply.setDoReconnect(reconnect);
        bus.reply(msg, reply);
    }

    private void handle(final ReconnectConsoleProxyMsg msg) {
        final ReconnectConsoleProxyReply reply = new ReconnectConsoleProxyReply();

        // Currently this message only do some local shell operations in management node,
        // so we skip it during unit test.
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            bus.reply(msg, reply);
            return;
        }

        doConnectAgent(msg.isFullDeploy(), new Completion(msg) {
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

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIReconnectConsoleProxyAgentMsg) {
            handle((APIReconnectConsoleProxyAgentMsg) msg);
        } else if (msg instanceof APIUpdateConsoleProxyAgentMsg){
            handle((APIUpdateConsoleProxyAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIReconnectConsoleProxyAgentMsg msg) {
        final APIReconnectConsoleProxyAgentEvent evt = new APIReconnectConsoleProxyAgentEvent(msg.getId());

        SimpleQuery<ManagementNodeVO> q = dbf.createQuery(ManagementNodeVO.class);
        q.select(ManagementNodeVO_.uuid);

        if (msg.getAgentUuids() != null) {
            q.add(ManagementNodeVO_.uuid, SimpleQuery.Op.IN, msg.getAgentUuids());
        }

        final List<String> mgmtNodeUuids = q.listValue();

        final Map<String, Object> errors = new HashMap<String, Object>();

        if (msg.getAgentUuids() != null) {
            for (String uuid : msg.getAgentUuids()) {
                if (!mgmtNodeUuids.contains(uuid)) {
                    errors.put(uuid, argerr("invalid management node UUID[%s]", uuid));
                }
            }
        }

        if (mgmtNodeUuids.isEmpty()) {
            evt.setInventory(errors);
            bus.publish(evt);
            return;
        }

        final List<ReconnectConsoleProxyMsg> rmsgs = transformAndRemoveNull(mgmtNodeUuids, arg -> {
            ReconnectConsoleProxyMsg rmsg = new ReconnectConsoleProxyMsg();
            rmsg.setAgentUuid(arg);
            bus.makeServiceIdByManagementNodeId(rmsg, ConsoleConstants.SERVICE_ID, arg);
            return rmsg;
        });

        bus.send(rmsgs, new CloudBusListCallBack(msg) {
            ErrorCodeList errorCodes = new ErrorCodeList();

            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    String mgmgUuid = mgmtNodeUuids.get(replies.indexOf(r));
                    if (r.isSuccess()) {
                        errors.put(mgmgUuid, true);
                    } else {
                        errors.put(mgmgUuid, r.getError());
                        errorCodes.add(r.getError());
                    }
                }
                if (errorCodes.hasError()) {
                    evt.setError(multiErr(errorCodes));
                }
                evt.setInventory(errors);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUpdateConsoleProxyAgentMsg msg) {
        final APIUpdateConsoleProxyAgentEvent evt = new APIUpdateConsoleProxyAgentEvent(msg.getId());

        UpdateConsoleProxyAgentMsg umsg = new UpdateConsoleProxyAgentMsg();
        umsg.setUuid(msg.getUuid());
        umsg.setConsoleProxyOverriddenIp(msg.getConsoleProxyOverriddenIp());
        umsg.setConsoleProxyPort(msg.getConsoleProxyPort());
        bus.makeServiceIdByManagementNodeId(umsg, ConsoleConstants.SERVICE_ID, msg.getUuid());
        bus.send(umsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(UpdateConsoleProxyAgentMsg msg) {
        final UpdateConsoleProxyAgentReply reply = new UpdateConsoleProxyAgentReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("update-console-proxy-agent-%s", msg.getUuid()));
        chain.then(new ShareFlow() {
            ConsoleProxyAgentVO vo;
            String oldProxyIp;
            int oldProxyPort;
            int newProxyPort = msg.getConsoleProxyPort() == null ? CoreGlobalProperty.CONSOLE_PROXY_PORT : msg.getConsoleProxyPort();


            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "verify-console-proxy-port";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (msg.getConsoleProxyPort() == null) {
                            trigger.next();
                            return;
                        }

                        vo = dbf.findByUuid(msg.getUuid(), ConsoleProxyAgentVO.class);
                        if ((int)msg.getConsoleProxyPort() == vo.getConsoleProxyPort()) {
                            trigger.next();
                            return;
                        }

                        ShellUtils.ShellRunner runner = new ShellUtils.ShellRunner();
                        runner.setCommand(String.format("netstat -nltp | grep -E ':%d\\s+'", msg.getConsoleProxyPort()));
                        runner.setVerbose(false);
                        runner.setWithSudo(true);
                        runner.setSuppressTraceLog(true);
                        ShellResult res = runner.run();
                        String stdout = res.getStdout();
                        if (res.getRetCode() == 0) {
                            trigger.fail(argerr("there is other process using the port: %s", stdout));
                        } else {
                            trigger.next();
                        }
                    }
                });
                flow(new Flow() {
                    String __name__ = "update-console-proxy-agent-vo";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vo = dbf.findByUuid(msg.getUuid(), ConsoleProxyAgentVO.class);
                        oldProxyIp = vo.getConsoleProxyOverriddenIp();
                        oldProxyPort = vo.getConsoleProxyPort();
                        vo.setConsoleProxyOverriddenIp(msg.getConsoleProxyOverriddenIp());
                        vo.setConsoleProxyPort(newProxyPort);
                        dbf.update(vo);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        vo = dbf.reload(vo);
                        vo.setConsoleProxyOverriddenIp(oldProxyIp);
                        vo.setConsoleProxyPort(oldProxyPort);
                        dbf.update(vo);
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "update-platform-global-properties";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Platform.getGlobalProperties().put("consoleProxyOverriddenIp", msg.getConsoleProxyOverriddenIp());
                        Platform.getGlobalProperties().put("consoleProxyPort", Integer.toString(newProxyPort));
                        CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP = msg.getConsoleProxyOverriddenIp();
                        CoreGlobalProperty.CONSOLE_PROXY_PORT = newProxyPort;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        Platform.getGlobalProperties().put("consoleProxyOverriddenIp", oldProxyIp);
                        Platform.getGlobalProperties().put("consoleProxyPort", Integer.toString(oldProxyPort));
                        CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP = oldProxyIp;
                        CoreGlobalProperty.CONSOLE_PROXY_PORT = oldProxyPort;
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "zstack-ctl-configure-consoleProxyOverriddenIp";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        int rst = setConsoleProxyOverridenIp(msg.getConsoleProxyOverriddenIp());
                        int portRst = setConsoleProxyPort(newProxyPort);
                        if (rst == 0) {
                            trigger.next();
                        } else {
                            trigger.fail(operr("failed to configure consoleProxyOverriddenIp[code:%d] or consoleProxyPort[code:%d]"));
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        setConsoleProxyOverridenIp(oldProxyIp);
                        setConsoleProxyPort(oldProxyPort);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "reconnect-console-proxy";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ReconnectConsoleProxyMsg rmsg = new ReconnectConsoleProxyMsg();
                        rmsg.setAgentUuid(msg.getUuid());
                        bus.makeServiceIdByManagementNodeId(rmsg, ConsoleConstants.SERVICE_ID, msg.getUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(operr("failed to reconnect console proxy"));
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        vo = dbf.reload(vo);
                        reply.setInventory(ConsoleProxyAgentInventory.valueOf(vo));
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

    @Override
    public boolean start() {
        tracker.track(Platform.getManagementServerId());
        return super.start();
    }


    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }
}
