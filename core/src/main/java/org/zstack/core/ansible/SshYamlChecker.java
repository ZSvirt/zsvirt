package org.zstack.core.ansible;

import org.apache.logging.log4j.util.Strings;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/12/14.
 */
public class SshYamlChecker implements AnsibleChecker {
    private static final CLogger logger = Utils.getLogger(SshYamlChecker.class);

    private String yamlFilePath;
    private String username;
    private String password;
    private String privateKey;
    private String targetIp;
    private int sshPort = 22;
    private Map<String, String> expectConfigs = new HashMap<>();

    @Override
    public boolean needDeploy() {
        if (Strings.isEmpty(yamlFilePath) || expectConfigs.isEmpty()) {
            return false;
        }

        Ssh ssh = new Ssh();
        ssh.setUsername(username).setPrivateKey(privateKey)
                .setPassword(password).setPort(sshPort)
                .setHostname(targetIp);
        try {

            ssh.sudoCommand(String.format("grep -o '%s' %s | uniq | wc -l", getGrepArgs(), yamlFilePath));
            SshResult ret = ssh.run();
            if (ret.getReturnCode() != 0) {
                logger.warn(String.format("exec ssh command failed, return code: %d, stdout: %s, stderr: %s",
                        ret.getReturnCode(), ret.getStdout(), ret.getStderr()));
                return true;
            }

            String out = ret.getStdout();
            if (Strings.isEmpty(out) || !out.trim().equals(String.valueOf(expectConfigs.size()))) {
                return true;
            }

            ssh.reset();
        } finally {
            ssh.close();
        }

        return false;
    }

    @Override
    public void deleteDestFile() {
        // do nothing.
    }

    public SshYamlChecker expectConfig(String key, String value) {
        expectConfigs.put(key, value);
        return this;
    }

    private String getGrepArgs() {
        return expectConfigs.entrySet().stream().map(it -> it.getKey() + "\\s*:\\s*" + it.getValue())
                .collect(Collectors.joining("\\|"));
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

    public String getYamlFilePath() {
        return yamlFilePath;
    }

    public void setYamlFilePath(String yamlFilePath) {
        this.yamlFilePath = yamlFilePath;
    }
}
