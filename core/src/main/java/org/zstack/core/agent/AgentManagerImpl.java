package org.zstack.core.agent;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleNeedRun;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFolderMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;

/**
 * Created by frank on 12/5/2015.
 */
public class AgentManagerImpl extends AbstractService implements AgentManager {
    private static final CLogger logger = Utils.getLogger(AgentManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private RESTFacade restf;

    private Map<String, Map<String, AgentStruct>> agents = new HashMap<String, Map<String, AgentStruct>>();
    private String srcRootFolder;

    public static final String ECHO_PATH = "/server/echo";
    public static final String INIT_PATH = "/server/init";

    public static final class InitAgentServerCmd {
        public Map<String, Object> Config = new HashMap<String, Object>();
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DeployAgentMsg) {
            handle((DeployAgentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    void init() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        PathUtil.forceCreateDirectory(AgentConstant.SRC_ANSIBLE_ROOT);
        File srcFolder = PathUtil.findFolderOnClassPath(AgentConstant.ANSIBLE_MODULE_PATH, true);
        srcRootFolder = srcFolder.getAbsolutePath();
        PathUtil.copyFolderToFolder(PathUtil.join(srcRootFolder, "server"), AgentConstant.SRC_ANSIBLE_ROOT);
    }

    private void handle(final DeployAgentMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("deploy-agent-to-server-%s", msg.getIp());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                deployAgent(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
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

    private void connect(final DeployAgentMsg msg, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("continue-connect-agent-server-%s:%s", msg.getIp(), msg.getAgentPort()));
        chain.then(new ShareFlow() {
            private String url(String path) {
                return String.format("http://%s:%s%s", msg.getIp(), msg.getAgentPort(), path);
            }

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "echo-server";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        restf.echo(url(ECHO_PATH), new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    String __name__= "init-server";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Map<String, Object> config = new HashMap<String, Object>();
                        config.put(AgentConstant.CONFIG_COMMAND_URL, restf.getSendCommandUrl());
                        if (msg.getConfig() != null) {
                            config.putAll(msg.getConfig());
                        }

                        InitAgentServerCmd cmd = new InitAgentServerCmd();
                        cmd.Config = config;

                        restf.syncJsonPost(url(INIT_PATH), cmd, Void.class);
                        trigger.next();
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

    private void deployAgent(final DeployAgentMsg msg, final NoErrorCompletion noErrorCompletion) {
        if (msg.getAgentPort() == null) {
            throw new CloudRuntimeException("agentPort cannot be null");
        }

        final DeployAgentReply reply = new DeployAgentReply();
        Map<String, AgentStruct> m = agents.get(msg.getOwner());
        if (m == null) {
            logger.warn(String.format("no plugins found for the agent[owner:%s], skip deploying agent", msg.getOwner()));
            bus.reply(msg, reply);
            noErrorCompletion.done();
            return;
        }

        try {
            String agentYamlPath = PathUtil.join(AgentConstant.ANSIBLE_MODULE_PATH, "server", AgentConstant.ANSIBLE_PLAYBOOK_NAME);
            File agentYaml = PathUtil.findFileOnClassPath(agentYamlPath, true);

            final File tmpInclude = File.createTempFile("zstack", "ansibleInclude");
            StringBuilder sb = new StringBuilder("---\n\n");
            for (AgentStruct s : m.values()) {
                sb.append(String.format("- include: %s\n", s.getAnsibleYaml()));
            }
            FileUtils.writeStringToFile(tmpInclude, sb.toString());

            String agentYamlContent = FileUtils.readFileToString(agentYaml);
            agentYamlContent = ln(agentYamlContent).formatByMap(map(
                    e("remoteRoot", AgentConstant.DST_ANSIBLE_ROOT),
                    e("srcRoot", srcRootFolder),
                    e("agentYamls", String.format("%s", tmpInclude.getAbsolutePath())),
                    e("outterServerIp", msg.getIp()),
                    e("outterServerPort", msg.getAgentPort().toString())
            ));

            final File tmpAgentYaml = File.createTempFile("zstack", "ansilbeTempAgent");
            FileUtils.writeStringToFile(tmpAgentYaml, agentYamlContent);

            SshFolderMd5Checker checker = new SshFolderMd5Checker();
            checker.setPassword(msg.getPassword());
            checker.setUsername(msg.getUsername());
            if (msg.getSshPort() != null) {
                checker.setPort(msg.getSshPort());
            }
            checker.setHostname(msg.getIp());
            checker.setSrcFolder(srcRootFolder);
            checker.setDstFolder(AgentConstant.DST_ANSIBLE_ROOT);
            final boolean fileChanged = checker.needDeploy();

            if (fileChanged) {
                Ssh ssh = new Ssh();
                ssh.setPassword(msg.getPassword());
                ssh.setUsername(msg.getUsername());
                ssh.setHostname(msg.getIp());
                if (msg.getSshPort() != null) {
                    ssh.setPort(msg.getSshPort());
                }
            }

            AnsibleRunner runner = new AnsibleRunner();
            runner.setAnsibleNeedRun(new AnsibleNeedRun() {
                @Override
                public boolean isRunNeed() {
                    return fileChanged || !NetworkUtils.isRemotePortOpen(msg.getIp(), msg.getAgentPort(), (int) TimeUnit.SECONDS.toMillis(5));
                }
            });
            runner.setPassword(msg.getPassword());
            runner.setUsername(msg.getUsername());
            runner.setAgentPort(msg.getAgentPort());
            runner.setRunOnLocal(true);
            runner.setFullDeploy(msg.isDeployAnyway());
            runner.setAnsibleExecutable("ansible-playbook");
            if (msg.getSshPort() != null) {
                runner.setSshPort(msg.getSshPort());
            }
            runner.setTargetIp(msg.getIp());
            runner.setPlayBookPath(tmpAgentYaml.getAbsolutePath());
            runner.run(new ReturnValueCompletion<Boolean>(msg, noErrorCompletion) {
                @Override
                @Deferred
                public void success(Boolean deployed) {
                    Defer.defer(new Runnable() {
                        @Override
                        public void run() {
                            if (!tmpInclude.delete()) {
                                logger.warn(String.format("failed to delete file[%s]", tmpInclude));
                            }
                            if (!tmpAgentYaml.delete()) {
                                logger.warn(String.format("failed to delete file[%s]", tmpAgentYaml));
                            }
                        }
                    });

                    connect(msg, new Completion(msg, noErrorCompletion) {
                        @Override
                        public void success() {
                            bus.reply(msg, reply);
                            noErrorCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            bus.reply(msg, reply);
                            noErrorCompletion.done();
                        }
                    });
                }

                @Override
                @Deferred
                public void fail(ErrorCode errorCode) {
                    Defer.defer(new Runnable() {
                        @Override
                        public void run() {
                            if (!tmpInclude.delete()) {
                                logger.warn(String.format("failed to delete file[%s]", tmpInclude));
                            }
                            if (!tmpAgentYaml.delete()) {
                                logger.warn(String.format("failed to delete file[%s]", tmpAgentYaml));
                            }
                        }
                    });

                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    noErrorCompletion.done();
                }
            });
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AgentConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void registerAgent(AgentStruct struct) {
        Map<String, AgentStruct> m = agents.computeIfAbsent(struct.getAgentOwner(), k -> new HashMap<String, AgentStruct>());

        AgentStruct old = m.get(struct.getAgentId());
        if (old != null) {
            throw new CloudRuntimeException(String.format("there has been an agent[id:%s] registered to the owner[%s]", struct.getAgentId(), struct.getAgentOwner()));
        }

        m.put(struct.getAgentId(), struct);
        PathUtil.copyFolderToFolder(struct.getFileFolder(), AgentConstant.SRC_ANSIBLE_ROOT);
    }
}
