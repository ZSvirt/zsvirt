package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.CallBackNetworkChecker;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.header.Component;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Create by weiwang at 2018/10/10
 */
public class SharedBlockAgentDeployer implements KVMHostConnectExtensionPoint, Component {
    private final static CLogger logger = Utils.getLogger(SharedBlockAgentDeployer.class);

    @Autowired
    private AnsibleFacade asf;

    @Override
    public boolean start() {
        deployAnsible();
        return true;
    }

    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(SharedBlockConstants.ANSIBLE_MODULE_PATH, SharedBlockConstants.ANSIBLE_PLAYBOOK_NAME);
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "deploy_zstack_sharedblock_agent_on_host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
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
                String srcAgentName = SharedBlockConstants.AGENT_PACKAGE_NAME;
                if (inv.getArchitecture() != null && !HostConstant.HOST_ARCHITECTURE_X86_64.equals(inv.getArchitecture())) {
                    srcAgentName = srcAgentName.replace("bin", inv.getArchitecture()+ ".bin");
                }
                checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("%s/%s", SharedBlockConstants.ANSIBLE_MODULE_PATH, srcAgentName), true).getAbsolutePath(),
                        String.format("/var/lib/zstack/zsblk-agent/package/%s", SharedBlockConstants.AGENT_PACKAGE_NAME));

                AnsibleRunner runner = new AnsibleRunner();
                runner.setAgentPort(SharedBlockGlobalProperty.AGENT_PORT);
                runner.installChecker(checker);
                runner.setPassword(password);
                runner.setUsername(username);
                runner.setTargetIp(hostname);
                runner.setTargetUuid(inv.getUuid());
                runner.setSshPort(port);
                runner.setPlayBookName(SharedBlockConstants.ANSIBLE_PLAYBOOK_NAME);

                SharedBlockDeployArguments deployArguments = new SharedBlockDeployArguments();
                //TODO: need to deploy agent if config changed
                deployArguments.setFreeSpace(SharedBlockGlobalConfig.THIN_PROVISIONING_VOLUME_FREESPACE.value(Long.class));
                deployArguments.setIncrement(SharedBlockGlobalConfig.THIN_PROVISIONING_VOLUME_INCREMENT.value(Long.class));
                deployArguments.setUtilizationPercent(SharedBlockGlobalConfig.THIN_PROVISIONING_VOLUME_UTILIZATION_PERCENT.value(Long.class));
                deployArguments.setMaxLockButNotUsedTimes(SharedBlockGlobalConfig.LOCK_HELPER_MAX_TIMES.value(Long.class));
                deployArguments.setLvLkProtectionPeriodInSec(SharedBlockGlobalConfig.LOCK_HELPER_PROTECTION_PERIOD.value(Integer.class));
                deployArguments.setScanInterval(SharedBlockGlobalConfig.LOCK_HELPER_SCAN_INTERVAL.value(Long.class));
                deployArguments.setVerboseLog(SharedBlockGlobalConfig.VERBOSE_LOG.value(String.class));
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
}
