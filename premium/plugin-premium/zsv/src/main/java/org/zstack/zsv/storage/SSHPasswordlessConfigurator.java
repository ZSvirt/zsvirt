package org.zstack.zsv.storage;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.zstack.core.Platform.operr;

public class SSHPasswordlessConfigurator {
    private static final CLogger logger = Utils.getLogger(SSHPasswordlessConfigurator.class);

    public static class HostInfo {
        private final String hostUuid;
        private final String ip;
        private final String username;
        private final String password;
        private final int port;

        public HostInfo(String hostUuid, String ip, String username, String password, int port) {
            if (StringUtils.isEmpty(hostUuid)) {
                throw new IllegalArgumentException("hostUuid cannot be null or empty");
            }
            if (StringUtils.isEmpty(ip)) {
                throw new IllegalArgumentException("ip address cannot be null or empty");
            }
            if (StringUtils.isEmpty(username)) {
                throw new IllegalArgumentException("username cannot be null or empty");
            }
            if (StringUtils.isEmpty(password)) {
                throw new IllegalArgumentException("password cannot be null or empty");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Invalid port number: " + port);
            }

            this.hostUuid = hostUuid;
            this.ip = ip;
            this.username = username;
            this.password = password;
            this.port = port;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public String getIp() {
            return ip;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public int getPort() {
            return port;
        }
    }

    public void setupPasswordlessSSH(List<HostInfo> hosts) {
        logger.info("Starting SSH passwordless setup using ssh-copy-id...");

        generateMissingSSHKeys(hosts);
        logger.info("SSH keys verified/generated on all hosts");

        configurePasswordlessSSH(hosts);
        logger.info("Passwordless SSH configured between all hosts");

        testPasswordlessSSH(hosts);
        logger.info("Passwordless SSH tests completed successfully");

        logger.info("SSH passwordless setup completed successfully!");
    }

    private void generateMissingSSHKeys(List<HostInfo> hosts) {
        for (HostInfo host : hosts) {
            Ssh ssh = createSshConnection(host);

            try {
                ssh.command("if [ -f ~/.ssh/id_rsa ] && [ -f ~/.ssh/id_rsa.pub ]; then echo 'exists'; " +
                        "elif [ ! -f ~/.ssh/id_rsa ] && [ ! -f ~/.ssh/id_rsa.pub ]; then echo 'missing'; else echo 'incomplete'; fi");
                SshResult result = ssh.run();
                if (result.getReturnCode() != 0) {
                    throw new OperationFailureException(operr("Failed to check SSH keys on host[%s:%d]",
                            host.getIp(), host.getPort())
                            .withOpaque("host.uuid", host.getHostUuid())
                            .withOpaque("bash.error", result.getStderr()));
                }

                if ("incomplete".equals(result.getStdout().trim())) {
                    throw new OperationFailureException(operr("SSH keys are incomplete on host[%s:%d].",
                            host.getIp(), host.getPort())
                            .withOpaque("host.uuid", host.getHostUuid())
                            .withOpaque("keys.incomplete", "Either ~/.ssh/id_rsa or ~/.ssh/id_rsa.pub is missing. " +
                                    "Please ensure both private and public keys exist or remove both to regenerate."));
                }

                if ("missing".equals(result.getStdout().trim())) {
                    logger.info(String.format("Generating SSH keys on host[%s:%s:%d]", host.getHostUuid(), host.getIp(), host.getPort()));
                    ssh.reset();
                    ssh.command("ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa -q");
                    result = ssh.run();
                    if (result.getReturnCode() != 0) {
                        throw new OperationFailureException(operr("Failed to generate SSH keys on host[%s:%d]",
                                host.getIp(), host.getPort())
                                .withOpaque("host.uuid", host.getHostUuid())
                                .withOpaque("bash.error", result.getStderr()));
                    }
                } else {
                    logger.info(String.format("SSH keys already exist on host[%s:%d]", host.getIp(), host.getPort()));
                }
            } finally {
                ssh.close();
            }
        }
    }

    private void configurePasswordlessSSH(List<HostInfo> hosts) {
        Map<String, Set<String>> existingConnections = findExistingConnections(hosts);

        for (HostInfo source : hosts) {
            for (HostInfo target : hosts) {
                if (source.getIp().equals(target.getIp())) {
                    continue;
                }
                if (isConnectionConfigured(existingConnections, source, target)) {
                    logger.info("Skipping configuration: " + source.getIp() + ":" + source.getPort() +
                            " -> " + target.getIp() + ":" + target.getPort() + " (already configured)");
                    continue;
                }
                logger.info("Configuring passwordless SSH from " + source.getIp() + ":" + source.getPort() +
                        " to " + target.getIp() + ":" + target.getPort());
                executeSSHCopyId(source, target);
            }
        }
    }

    private void executeSSHCopyId(HostInfo source, HostInfo target) {
        Ssh ssh = createSshConnection(source);

        try {
            String tmpPwFile = String.format("/tmp/%s_sds_%s", source.getHostUuid(), target.getHostUuid());
            String base64Content = Base64.getEncoder().encodeToString(target.getPassword().getBytes(StandardCharsets.UTF_8));

            String command = String.format(
                    "echo '%s' | base64 -d > %s && " +
                            "chmod 600 %s && " +
                            "sshpass -f %s ssh-copy-id -o StrictHostKeyChecking=no -o ConnectTimeout=30 -f -p %d %s@%s; " +
                            "rm -f %s",
                    base64Content,
                    tmpPwFile,
                    tmpPwFile,
                    tmpPwFile,
                    target.getPort(),
                    target.getUsername(),
                    target.getIp(),
                    tmpPwFile
            );

            ssh.command(command);
            SshResult result = ssh.run();

            if (result.getReturnCode() != 0) {
                throw new OperationFailureException(operr("ssh-copy-id failed from " + source.getIp() + ":" + source.getPort() +
                        " to " + target.getIp()).withOpaque("bash.error", result.getStderr()));
            }

            logger.info("Successfully configured SSH from " + source.getIp() + ":" + source.getPort() +
                    " to " + target.getIp() + ":" + target.getPort());
        } finally {
            ssh.close();
        }
    }

    private void testPasswordlessSSH(List<HostInfo> hosts) {
        for (HostInfo source : hosts) {
            for (HostInfo target : hosts) {
                if (source.getIp().equals(target.getIp())) {
                    continue;
                }

                Ssh ssh = createSshConnection(source);
                try {
                    ssh.command("ssh -o PasswordAuthentication=no -o BatchMode=yes -o StrictHostKeyChecking=no -o ConnectTimeout=30 " +
                            "-p " + target.getPort() + " " + target.getUsername() + "@" + target.getIp() + " 'echo passwordless-ssh-verified'");

                    SshResult result = ssh.run();

                    if (result.getReturnCode() != 0 || !result.getStdout().trim().contains("passwordless-ssh-verified")) {
                        throw new OperationFailureException(operr("Passwordless SSH test failed from " +
                                source.getIp() + ":" + source.getPort() + " to " +
                                target.getIp() + ":" + target.getPort()).withOpaque("bash.error", result.getStderr()));
                    }

                    logger.info("Success: " + source.getIp() + ":" + source.getPort() +
                            " -> " + target.getIp() + ":" + target.getPort());
                } finally {
                    ssh.close();
                }
            }
        }
    }

    private Map<String, Set<String>> findExistingConnections(List<HostInfo> hosts) {
        Map<String, Set<String>> connections = new HashMap<>();

        for (HostInfo host : hosts) {
            Set<String> connectedHosts = new HashSet<>();

            for (HostInfo target : hosts) {
                if (!host.getIp().equals(target.getIp())) {
                    Ssh ssh = createSshConnection(host);

                    try {
                        ssh.command("ssh -o PasswordAuthentication=no -o BatchMode=yes -o ConnectTimeout=30 " +
                                "-p " + target.getPort() + " " + target.getUsername() + "@" + target.getIp() + " 'exit'");

                        SshResult result = ssh.run();
                        if (result.getReturnCode() == 0) {
                            connectedHosts.add(target.getIp());
                        }
                    } finally {
                        ssh.close();
                    }
                }
            }

            connections.put(host.getIp(), connectedHosts);
        }

        return connections;
    }

    private boolean isConnectionConfigured(Map<String, Set<String>> connections, HostInfo source, HostInfo target) {
        String sourceKey = source.getIp();
        String targetKey = target.getIp();

        Set<String> sourceConnections = connections.get(sourceKey);
        return sourceConnections != null && sourceConnections.contains(targetKey);
    }

    private Ssh createSshConnection(HostInfo host) {
        Ssh ssh = new Ssh();
        ssh.setHostname(host.getIp())
                .setUsername(host.getUsername())
                .setPassword(host.getPassword())
                .setPort(host.getPort())
                .setTimeout(30);
        return ssh;
    }
}
