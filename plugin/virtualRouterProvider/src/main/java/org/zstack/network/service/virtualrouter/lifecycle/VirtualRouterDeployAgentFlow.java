package org.zstack.network.service.virtualrouter.lifecycle;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitRsp;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterDeployAgentFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VirtualRouterDeployAgentFlow.class);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ErrorFacade errf;

    private final String agentPackageName = VirtualRouterGlobalProperty.AGENT_PACKAGE_NAME;

    private void continueConnect(final VmNicInventory mgmtNic, final Map<String, Object> data, final FlowTrigger completion) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("virtual-router-%s-continue-connecting", mgmtNic.getVmInstanceUuid()));
        chain.then(new ShareFlow() {
            Long timeout = data.get(ApplianceVmConstant.Params.timeout.toString()) == null ?
                    null : (Long)data.get(ApplianceVmConstant.Params.timeout.toString());
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "echo";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_ECHO_PATH);
                        if (timeout == null) {
                            restf.echo(url, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        } else {
                            restf.echo(url, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            }, 100, timeout);
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_INIT);
                        InitCommand cmd = new InitCommand();
                        cmd.setUuid(vr.getUuid());
                        cmd.setRestartDnsmasqAfterNumberOfSIGUSER1(VirtualRouterGlobalConfig.RESTART_DNSMASQ_COUNT.value(Integer.class));
                        if (timeout == null) {
                            restf.asyncJsonPost(url, cmd, new JsonAsyncRESTCallback<InitRsp>(trigger) {
                                @Override
                                public void fail(ErrorCode err) {
                                    trigger.fail(err);
                                }

                                @Override
                                public void success(InitRsp ret) {
                                    if (ret.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("operation error, because:%s", ret.getError()));
                                    }
                                }

                                @Override
                                public Class<InitRsp> getReturnClass() {
                                    return InitRsp.class;
                                }
                            });
                        } else {
                            restf.asyncJsonPost(url, cmd, new JsonAsyncRESTCallback<InitRsp>(trigger) {
                                @Override
                                public void fail(ErrorCode err) {
                                    trigger.fail(err);
                                }

                                @Override
                                public void success(InitRsp ret) {
                                    if (ret.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("operation error, because:%s", ret.getError()));
                                    }
                                }

                                @Override
                                public Class<InitRsp> getReturnClass() {
                                    return InitRsp.class;
                                }
                            }, TimeUnit.MILLISECONDS, timeout);
                        }
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.next();
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

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory mgmtNic = null;
        if (vr != null) {
            mgmtNic = vr.getManagementNic();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            mgmtNic = CollectionUtils.findOneOrNull(spec.getDestNics(),
                    arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
            DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic for virtual router[uuid:%s, name:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName()));
        }

        boolean isReconnect = Boolean.parseBoolean((String) data.get(Param.IS_RECONNECT.toString()));

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(mgmtNic, data, chain);
            return;
        } else if (!isReconnect &&  !VirtualRouterGlobalConfig.DEPLOY_AGENT_ON_START.value(Boolean.class)) {
            continueConnect(mgmtNic, data, chain);
            return;
        }

        final String username = "root";
        final String privKey = asf.getPrivateKey();
        final String mgmtIp = mgmtNic.getIp();

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(mgmtIp);
        checker.setUsername(username);
        checker.setPrivateKey(privKey);
        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/virtualrouter/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/virtualrouter/%s", agentPackageName), true).getAbsolutePath(),
                String.format("/var/lib/zstack/virtualrouter/%s", agentPackageName));

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setUsername(username);
        runner.setPlayBookName(VirtualRouterConstant.ANSIBLE_PLAYBOOK_NAME);
        runner.setPrivateKey(privKey);
        runner.setAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);
        runner.setTargetIp(mgmtIp);
        final VmNicInventory fmgmtNic = mgmtNic;
        runner.setDeployArguments(new VirtualRouterDeployArguments());
        runner.run(new ReturnValueCompletion<Boolean>(chain) {
            @Override
            public void success(Boolean deployed) {
                continueConnect(fmgmtNic, data, chain);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        });
    }
}
