package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmCommands.InitCmd;
import org.zstack.appliancevm.ApplianceVmCommands.InitRsp;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmDeployAgentFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private AnsibleFacade asf;

    private void continueConnect(final String echoUrl, final String apvmUuid, final FlowTrigger outerTrigger) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("continue-connect-appliance-vm");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "echo";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        restf.echo(echoUrl, new Completion(trigger) {
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
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        InitCmd cmd = new InitCmd();

                        ApplianceVmAsyncHttpCallMsg msg = new ApplianceVmAsyncHttpCallMsg();
                        msg.setVmInstanceUuid(apvmUuid);
                        msg.setCommand(cmd);
                        msg.setCheckStatus(false);
                        msg.setPath(ApplianceVmConstant.INIT_PATH);
                        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, apvmUuid);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                ApplianceVmAsyncHttpCallReply ar = reply.castReply();
                                InitRsp rsp = ar.toResponse(InitRsp.class);
                                if (!rsp.isSuccess()) {
                                    trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    return;
                                }

                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(outerTrigger) {
                    @Override
                    public void handle(Map data) {
                        outerTrigger.next();
                    }
                });

                error(new FlowErrorHandler(outerTrigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        outerTrigger.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        boolean isReconnect = Boolean.parseBoolean((String) data.get(Params.isReconnect.toString()));
        final String apvmUuid;

        String mgmtNicIp = null;
        if (!isReconnect) {
            VmNicInventory mgmtNic;
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.findOneOrNull(spec.getDestNics(), arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
                apvmUuid = spec.getVmInventory().getUuid();
            } else {
                ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
                apvmUuid = avo.getUuid();
            }
            if (mgmtNic != null) {
                mgmtNicIp = mgmtNic.getIp();
            }
        } else {
            mgmtNicIp = (String) data.get(Params.managementNicIp.toString());
            apvmUuid = (String) data.get(Params.applianceVmUuid.toString());
        }

        final String mgmtIp = mgmtNicIp;
        final String url = ApplianceVmBase.buildAgentUrl(mgmtIp, ApplianceVmConstant.ECHO_PATH, ApplianceVmGlobalProperty.AGENT_PORT);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(url, apvmUuid, trigger);
            return;
        } else if (!isReconnect && !ApplianceVmGlobalConfig.DEPLOY_AGENT_ON_START.value(Boolean.class)) {
            continueConnect(url, apvmUuid, trigger);
            return;
        }

        final String username = "root";
        final String privKey = asf.getPrivateKey();

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(mgmtIp);
        checker.setUsername(username);
        checker.setPrivateKey(privKey);
        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/appliancevm/package/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
        checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/appliancevm/%s", ApplianceVmGlobalProperty.AGENT_PACKAGE_NAME), true).getAbsolutePath(),
                String.format("/var/lib/zstack/appliancevm/package/%s", ApplianceVmGlobalProperty.AGENT_PACKAGE_NAME));

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setUsername(username);
        runner.setPlayBookName(ApplianceVmConstant.ANSIBLE_PLAYBOOK_NAME);
        runner.setPrivateKey(privKey);
        runner.setAgentPort(ApplianceVmGlobalProperty.AGENT_PORT);
        runner.setTargetIp(mgmtIp);
        runner.setDeployArguments(new ApplianceVmDeployArguments());
        runner.run(new ReturnValueCompletion<Boolean>(trigger) {
            @Override
            public void success(Boolean deployed) {
                continueConnect(url, apvmUuid, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }
}
