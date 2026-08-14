package org.zstack.core.ansible;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

public class SshFileExistChecker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(SshFileExistChecker.class);

    private String filePath;
    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private int sshPort = 22;

    @Override
    public boolean needDeploy() {
        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp)
                .setTimeout(5);
        try {
            ssh.sudoCommand(String.format("stat %s", filePath));
            SshResult ret = ssh.run();
            if (ret.getReturnCode() != 0) {
                logger.debug(String.format("file not exist, file: %s", filePath));
                return true;
            }
        } finally {
            ssh.close();
        }

        return false;
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

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
