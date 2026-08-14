package org.zstack.core.ansible;

import org.zstack.core.CoreGlobalProperty;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.List;

public class SshFileMd5Checker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(SshFileMd5Checker.class);

    private List<SrcDestPair> srcDestPairs = new ArrayList<SrcDestPair>();
    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private int sshPort = 22;

    private class SrcDestPair {
        private SrcDestPair(String srcPath, String destPath) {
            this.srcPath = srcPath;
            this.destPath = destPath;
        }

        String srcPath;
        String destPath;
    }

    public static final String ZSTACKLIB_SRC_PATH = CoreGlobalProperty.UNIT_TEST_ON ? "/tmp" :
            PathUtil.findFileOnClassPath(String.format("ansible/zstacklib/%s",
                    AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME), true).getAbsolutePath();

    @Override
    public boolean needDeploy() {
        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp)
                .setTimeout(5);
        try {
            for (SrcDestPair b : srcDestPairs) {
                String sourceFilePath = b.srcPath;
                String destFilePath = b.destPath;

                ssh.sudoCommand(String.format("md5sum %s", destFilePath));
                SshResult ret = ssh.run();
                if (ret.getReturnCode() != 0) {
                    logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                            ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                    return true;
                }
                ssh.reset();

                String destMd5 =  ret.getStdout().split(" ")[0];
                ShellResult sret = ShellUtils.runAndReturn(String.format("md5sum %s", sourceFilePath));
                sret.raiseExceptionIfFail();
                String srcMd5 = sret.getStdout().split(" ")[0];
                if (!destMd5.equals(srcMd5)) {
                    logger.debug(String.format("file MD5 changed, src[%s, md5:%s] dest[%s, md5: %s]", sourceFilePath,
                            srcMd5, destFilePath, destMd5));
                    return true;
                }
            }
        } finally {
            ssh.close();
        }

        return false;
    }

    @Override
    public void deleteDestFile() {
        for (SrcDestPair b : srcDestPairs) {
            String destFilePath = b.destPath;
            if (!destFilePath.contains("zstack")) {
                logger.debug(String.format("skip delete dest file[%s] which is not zstack file", destFilePath));
                continue;
            }

            Ssh ssh = new Ssh();
            ssh.setUsername(username).setPrivateKey(privateKey)
                    .setPassword(password).setPort(sshPort)
                    .setHostname(targetIp).sudoCommand(String.format("rm -f %s", destFilePath)).runAndClose();
            logger.debug(String.format("delete dest file[%s]", destFilePath));
        }
    }

    public void addSrcDestPair(String srcFilePath, String destFilePath) {
        srcDestPairs.add(new SrcDestPair(srcFilePath, destFilePath));
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
}
