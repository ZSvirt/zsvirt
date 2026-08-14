package org.zstack.premium.externalservice.loki;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.externalservice.LocalServiceUnitConfig;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.storage.ceph.CephMonExtensionPoint;
import org.zstack.storage.ceph.backup.CephBackupStorageMonInventory;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.SshResult;
import org.zstack.utils.ssh.SshShell;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/9/9.
 */
public class PromtailFactory implements KVMHostConnectExtensionPoint, CephMonExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PromtailFactory.class);

    private boolean needRestart = false;

    private String writeConfigContent(LocalServiceUnitConfig config) {
        return String.format("sudo touch %s;sudo chmod 777 %s;sudo echo \"%s\" > %s;sudo chmod 644 %s;sudo sync", config.getConfigFilePath(),
                config.getConfigFilePath(), config.getConfigFileContent(), config.getConfigFilePath(), config.getConfigFilePath());
    }

    private String writeServiceUnitContent(LocalServiceUnitConfig config) {
        return String.format("sudo touch %s;sudo chmod 777 %s;sudo echo \"%s\" > %s;sudo chmod 644 %s;sudo sync;for i in 1 2 3; do sudo systemctl daemon-reload && break || sleep 5; done", config.getServiceUnitPath(),
                config.getServiceUnitPath(), config.getServiceUnitContent(), config.getServiceUnitPath(), config.getServiceUnitPath());
    }

    private boolean needRestart() {
        return needRestart;
    }

    public void deployPromtailService(String resourceUuid, String ip, String username, String password, int sshPort, Completion completion) {
        if (LokiGlobalProperty.LOKI_VERSION_MODE.equals("none")) {
            completion.success();
            return;
        }
        String hostCpuModelName = KVMSystemTags.CPU_MODEL_NAME.getTokenByResourceUuid(
                resourceUuid, KVMSystemTags.CPU_MODEL_NAME_TOKEN
        );
        if (hostCpuModelName != null && hostCpuModelName.contains("aarch64")) {
            completion.success();
            return;
        }
        
        if (CoreGlobalProperty.UNIT_TEST_ON || System.getProperty("os.arch").equals("aarch64")) {
            completion.success();
            return;
        }

        PromtailImpl03 promtail = new PromtailImpl03(ip);
        LocalServiceUnitConfig config = promtail.getConfigurations();

        SshShell sshShell = new SshShell();
        sshShell.setHostname(ip);
        sshShell.setUsername(username);
        sshShell.setPassword(password);
        sshShell.setPort(sshPort);
        // step 1: check if process on
        SshResult ret = sshShell.runCommand("sudo systemctl status promtail-server");
        if (ret.isSshFailure()) {
            completion.fail(operr("ssh failed"));
            return;
        }
        if (ret.getReturnCode() != 0) {
            needRestart = true;
        }

        // step 2: check if config changed
        ret = sshShell.runCommand(String.format("sudo cat %s", config.getConfigFilePath()));
        if (ret.isSshFailure()) {
            completion.fail(operr("ssh failed"));
            return;
        }
        if (ret.getReturnCode() != 0 || !ret.getStdout().trim().equals(config.getConfigFileContent().trim())) {
            needRestart = true;
            logger.debug(String.format("%s has been changed, re-write it", config.getConfigFilePath()));
            sshShell.runCommand(writeConfigContent(config));
        }

        // step 3: check if service config changed
        ret = sshShell.runCommand(String.format("sudo cat %s", config.getServiceUnitPath()));
        if (ret.isSshFailure()) {
            completion.fail(operr("ssh failed"));
            return;
        }
        if (ret.getReturnCode() != 0 || !ret.getStdout().trim().equals(config.getServiceUnitContent().trim())) {
            needRestart = true;
            logger.debug(String.format("%s has been changed, re-write it", config.getServiceUnitPath()));
            sshShell.runCommand(writeServiceUnitContent(config));
        }


        String srcPath = PathUtil.findFileOnClassPath(String.format("ansible/promtail/%s", LokiConstant.PROMTAIL_BIN_SRC_PATH), true).getAbsolutePath();
        String destPath = LokiConstant.PROMTAIL_BIN_PATH;
        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setUsername(username);
        checker.setPassword(password);
        checker.setSshPort(sshPort);
        checker.setTargetIp(ip);
        checker.addSrcDestPair(srcPath, destPath);

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setPlayBookName(LokiGlobalProperty.PROMTAIL_PLAYBOOK_NAME);
        runner.setTargetIp(ip);
        runner.setTargetUuid(resourceUuid);
        runner.setUsername(username);
        runner.setPassword(password);
        runner.setSshPort(sshPort);
        runner.setAgentPort(LokiGlobalProperty.PROMTAIL_PORT);

        PromtailDeployArguments deployArguments = new PromtailDeployArguments();
        deployArguments.setSrcPromtailBin(srcPath);
        deployArguments.setDstPromtailBin(destPath);
        deployArguments.setPkgPromtail(String.format("ansible/promtail/%s", LokiConstant.PROMTAIL_BIN_SRC_PATH));
        runner.setDeployArguments(deployArguments);
        runner.run(new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean run) {
                if (run || needRestart()) {
                    SshShell sshShell = new SshShell();
                    sshShell.setHostname(ip);
                    sshShell.setUsername(username);
                    sshShell.setPassword(password);
                    sshShell.setPort(sshPort);
                    sshShell.runCommand("sudo systemctl restart promtail-server");
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        final KVMHostInventory kvm = context.getInventory();
        return new NoRollbackFlow() {
            String __name__ = String.format("setup-loki-for-kvm-host-%s", kvm.getUuid());
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                if (kvm == null) {
                    trigger.next();
                    return;
                }
                deployPromtailService(kvm.getUuid(), kvm.getManagementIp(), kvm.getUsername(), kvm.getPassword(), kvm.getSshPort(), new Completion(trigger) {
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
        };
    }

    @Override
    public void addMoreAgentInPrimaryStorage(CephPrimaryStorageMonInventory inventory, Completion completion) {
        deployPromtailService(inventory.getMonUuid(), inventory.getHostname(), inventory.getSshUsername(), inventory.getSshPassword(), inventory.getSshPort(), completion);
    }

    @Override
    public void addMoreAgentInBackupStorage(CephBackupStorageMonInventory inventory, Completion completion) {
        deployPromtailService(inventory.getMonUuid(), inventory.getHostname(), inventory.getSshUsername(), inventory.getSshPassword(), inventory.getSshPort(), completion);
    }
}
