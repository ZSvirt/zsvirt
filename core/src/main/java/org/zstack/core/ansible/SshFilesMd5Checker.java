package org.zstack.core.ansible;

import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.List;

public class SshFilesMd5Checker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(SshFilesMd5Checker.class);

    private String username;
    private String password;
    private String privateKey;
    private String ip;
    private int sshPort = 22;

    private List<String> fileMd5sums = new ArrayList<>();
    String filePath;

    @Override
    public boolean needDeploy() {
        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(ip)
                .setTimeout(5);
        try {
            ssh.sudoCommand(String.format("md5sum %s", filePath));
            SshResult ret = ssh.run();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }
            ssh.reset();
            String md5 = ret.getStdout().split(" ")[0];
            if (!fileMd5sums.contains(md5)) {
                logger.debug(String.format("file MD5 changed, dest[%s, md5, %s]", filePath, md5));
                return true;
            }
        } finally {
            ssh.close();
        }

        return false;
    }

    @Override
    public void deleteDestFile() {
        if (!filePath.contains("zstack")) {
            logger.debug(String.format("skip delete dest file[%s] which is not zstack file", filePath));
            return;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(ip).sudoCommand(String.format("rm -f %s", filePath)).runAndClose();
        logger.debug(String.format("delete dest file[%s]", filePath));
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<String> getFileMd5sums() {
        return fileMd5sums;
    }

    public void setFileMd5sums(List<String> fileMd5sums) {
        this.fileMd5sums = fileMd5sums;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
