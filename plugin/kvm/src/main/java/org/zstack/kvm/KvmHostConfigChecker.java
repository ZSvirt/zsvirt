package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.ansible.AnsibleChecker;
import org.zstack.core.ansible.CallBackNetworkChecker;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KvmHostConfigChecker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(KvmHostConfigChecker.class);

    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private String requireKsmCheck;
    private int sshPort = 22;

    @Override
    public boolean needDeploy() {
        if ("none".equals(requireKsmCheck)) {
            return false;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp);
        try {
            ssh.sudoCommand("cat /sys/kernel/mm/ksm/run");
            SshResult ret = ssh.setTimeout(60).runAndClose();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }

            boolean ksmEnabledOnHost = "1".equals(ret.getStdout());
            if (ksmEnabledOnHost && "true".equals(requireKsmCheck)) {
                return false;
            }

            if (!ksmEnabledOnHost && "false".equals(requireKsmCheck)) {
                return false;
            }

            logger.debug(String.format("KSM status is %s (%s), but requireKsmCheck is %s, need to re-deploy",
                    ret.getStdout(),
                    ksmEnabledOnHost ? "enabled" : "disabled",
                    requireKsmCheck)
            );

            ssh.reset();
        } finally {
            ssh.close();
        }

        return true;
    }

    @Override
    public void deleteDestFile() {

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getRequireKsmCheck() {
        return requireKsmCheck;
    }

    public void setRequireKsmCheck(String requireKsmCheck) {
        this.requireKsmCheck = requireKsmCheck;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
