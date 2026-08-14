package org.zstack.zmigrate.client;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.thread.AsyncTimer;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.zmigrate.ZMigrateGlobalConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.zmigrate.ZMigrateConstant.*;
import static org.zstack.zmigrate.ZMigratePluginErrors.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZMigrateSshClient {
    private static final CLogger logger = Utils.getLogger(ZMigrateSshClient.class);

    private static String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /**
     * Write the decoded SSH password to a temporary file on /dev/shm (tmpfs,
     * RAM-backed) so it never touches persistent storage.  The file is created
     * with 0600 permissions.  Caller MUST delete via {@link #deleteLocalTempFile}
     * in a finally block.
     */
    private static String writePasswordToLocalTempFile(String base64Password) throws IOException {
        byte[] decoded = Base64.getDecoder().decode(base64Password);
        try {
            Path shmDir = Paths.get("/dev/shm");
            Path tmpFile;
            if (Files.isDirectory(shmDir)) {
                tmpFile = Files.createTempFile(shmDir, "ssh_", ".tmp",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } else {
                tmpFile = Files.createTempFile("ssh_", ".tmp",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            }
            Files.write(tmpFile, decoded);
            return tmpFile.toAbsolutePath().toString();
        } finally {
            Arrays.fill(decoded, (byte) 0);
        }
    }

    private static void deleteLocalTempFile(String path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            logger.warn(String.format("failed to delete local temp file: %s", path));
        }
    }

    /**
     * Build an sshpass command that reads the SSH password from a local temp
     * file ({@code sshpass -f}) instead of piping it through the command line.
     * This prevents the password (even base64-encoded) from appearing in
     * {@code /proc/PID/cmdline} or {@code ps} output.
     */
    private static String buildSshPassCmd(String localPasswdFile, int port, String username, String hostname, String remoteCmd) {
        return buildSshPassCmd(localPasswdFile, port, username, hostname, remoteCmd, 10);
    }

    private static String buildSshPassCmd(String localPasswdFile, int port, String username,
                                          String hostname, String remoteCmd, long timeoutSeconds) {
        // StrictHostKeyChecking=no is intentional for infrastructure automation:
        // gateway hosts are dynamically provisioned VMs whose host keys change
        // on re-deployment.  The connection is within a trusted management network.
        return String.format(
                "timeout %d sshpass -f %s ssh -p %d"
                        + " -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null %s@%s %s",
                timeoutSeconds, shellQuote(localPasswdFile), port, username, hostname, shellQuote(remoteCmd));
    }

    /**
     * Execute a remote command via sshpass + SSH.
     * Password is read from a temp file on /dev/shm — never appears in cmdline.
     */
    private ShellResult runSshCommand(String hostname, String remoteCmd) {
        String base64Password = ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value();
        String localPasswdFile = null;
        try {
            localPasswdFile = writePasswordToLocalTempFile(base64Password);
            String cmd = buildSshPassCmd(localPasswdFile, GATEWAY_SSH_PORT, GATEWAY_SSH_USERNAME, hostname, remoteCmd);

            ShellUtils.ShellRunner runner = new ShellUtils.ShellRunner();
            runner.setCommand(cmd);
            runner.setWithSudo(false);
            runner.setSuppressTraceLog(true);
            return runner.run();
        } catch (IOException e) {
            logger.warn(String.format("failed to create local temp password file: %s", e.getMessage()));
            ShellResult result = new ShellResult();
            result.setRetCode(-1);
            result.setStderr("failed to create local temp password file: " + e.getMessage());
            result.setStdout("");
            return result;
        } finally {
            deleteLocalTempFile(localPasswdFile);
        }
    }

    /**
     * Execute a remote command via SSH, piping the given data to its stdin.
     * Uses {@code sshpass -f <localTmpFile>} for SSH auth so that stdin is
     * free for piping data (e.g. password for {@code sudo -S}).
     * <p>
     * This is the Java equivalent of the cephagent.py pattern:
     * <pre>
     *   process = subprocess.Popen(sshpass_cmd, stdin=subprocess.PIPE, ...)
     *   process.communicate(input=data)
     * </pre>
     */
    private ShellResult runSshCommandWithStdin(String hostname, String remoteCmd, byte[] stdinData) {
        String base64Password = ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value();
        String localPasswdFile = null;
        try {
            localPasswdFile = writePasswordToLocalTempFile(base64Password);
            String sshCmd = buildSshPassCmd(localPasswdFile, GATEWAY_SSH_PORT, GATEWAY_SSH_USERNAME, hostname, remoteCmd);

            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", sshCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdinData);
                    os.flush();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream is = process.getInputStream()) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        baos.write(buf, 0, n);
                    }
                }
                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    ShellResult result = new ShellResult();
                    result.setRetCode(-1);
                    result.setStderr("SSH command timed out after 60 seconds");
                    result.setStdout("");
                    return result;
                }
                int rc = process.exitValue();

                ShellResult result = new ShellResult();
                result.setRetCode(rc);
                result.setStderr(baos.toString());
                result.setStdout("");
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                ShellResult result = new ShellResult();
                result.setRetCode(-1);
                result.setStderr("interrupted while executing SSH command with stdin");
                result.setStdout("");
                return result;
            } finally {
                process.destroy();
            }
        } catch (IOException e) {
            logger.warn(String.format("failed in runSshCommandWithStdin: %s", e.getMessage()));
            ShellResult result = new ShellResult();
            result.setRetCode(-1);
            result.setStderr("IO error: " + e.getMessage());
            result.setStdout("");
            return result;
        } finally {
            deleteLocalTempFile(localPasswdFile);
        }
    }

    public ShellResult run(String hostname, String remoteCmd, long timeoutSeconds) {
        return run(hostname, remoteCmd, null, timeoutSeconds);
    }

    public ShellResult runWithStdin(String hostname, String remoteCmd,
                                    byte[] stdinData, long timeoutSeconds) {
        return run(hostname, remoteCmd, stdinData, timeoutSeconds);
    }

    public ShellResult runWithSudoPassword(String hostname, String remoteCmd, long timeoutSeconds) {
        byte[] password = Base64.getDecoder().decode(ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
        try {
            return redactPassword(runWithStdin(hostname, remoteCmd, password, timeoutSeconds), password);
        } finally {
            Arrays.fill(password, (byte) 0);
        }
    }

    public ShellResult distributeVddk(String managementIp, String targetIp, String uploadTaskUuid,
                                      long timeoutSeconds) {
        if (!NetworkUtils.isValidIPAddress(targetIp) && !NetworkUtils.isHostname(targetIp)) {
            return shellError("invalid VDDK target host");
        }
        String uploadPath;
        String generationPath;
        try {
            String distributionUuid = UUID.randomUUID().toString().replace("-", "");
            uploadPath = vddkDistributionUploadPath(uploadTaskUuid, distributionUuid);
            generationPath = vddkGenerationPath(uploadTaskUuid);
        } catch (IllegalArgumentException e) {
            return shellError(e.getMessage());
        }
        if (managementIp.equals(targetIp)) {
            return runWithSudoPassword(
                    targetIp, buildVddkInstallCommand(generationPath, uploadTaskUuid),
                    VDDK_INSTALL_TIMEOUT_SECONDS);
        }

        byte[] password = Base64.getDecoder().decode(ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
        try {
            String command = buildVddkDistributionCommand(
                    targetIp, generationPath, uploadPath, uploadTaskUuid);
            return redactPassword(runWithStdin(managementIp, command, password, timeoutSeconds), password);
        } finally {
            Arrays.fill(password, (byte) 0);
        }
    }

    private static String buildVddkInstallCommand(String sourcePath, String uploadTaskUuid) {
        return String.format("sudo -S -p '' sh -c %s",
                shellQuote(buildVddkLockedInstallScript(sourcePath, uploadTaskUuid)));
    }

    static String buildVddkLockedInstallScript(String sourcePath, String uploadTaskUuid) {
        return String.format("flock -n %s sh -c %s",
                shellQuote(VDDK_INSTALL_LOCK_PATH),
                shellQuote(buildVddkInstallScript(sourcePath, uploadTaskUuid)));
    }

    static String buildVddkInstallScript(String sourcePath, String uploadTaskUuid) {
        vddkGenerationPath(uploadTaskUuid);
        return String.format(
                "marker=%s; " +
                        "if test -f \"$marker\" && test ! -L \"$marker\" && grep -qx %s \"$marker\"; " +
                        "then :; else " +
                        "test -f %s && test ! -L %s && " +
                        "docker cp %s %s && docker restart treker >/dev/null && " +
                        "sleep 90 && docker restart treker >/dev/null && " +
                        "docker inspect -f '{{.State.Running}}' treker | grep -qx true && " +
                        "marker_tmp=$(mktemp \"${marker}.XXXXXX\") && " +
                        "printf '%%s\\n' %s > \"$marker_tmp\" && chmod 600 \"$marker_tmp\" && " +
                        "mv -T -- \"$marker_tmp\" \"$marker\"; fi",
                shellQuote(VDDK_GENERATION_MARKER_PATH), shellQuote(uploadTaskUuid),
                shellQuote(sourcePath), shellQuote(sourcePath), shellQuote(sourcePath),
                shellQuote("treker:" + VDDK_CONTAINER_PATH), shellQuote(uploadTaskUuid));
    }

    private static String buildVddkDistributionCommand(String targetIp, String generationPath,
                                                        String uploadPath, String uploadTaskUuid) {
        String targetInstallCommand = String.format(
                "sudo -S -p '' sh -c %s",
                shellQuote(String.format("rc=0; %s || rc=$?; rm -f -- %s; exit $rc",
                        buildVddkLockedInstallScript(uploadPath, uploadTaskUuid),
                        shellQuote(uploadPath))));
        return String.format(
                "set -eu\n" +
                        "umask 077\n" +
                        "password_file=$(mktemp /dev/shm/zmigrate-vddk-password.XXXXXX)\n" +
                        "askpass_file=$(mktemp /dev/shm/zmigrate-vddk-askpass.XXXXXX)\n" +
                        "source_stage=$(mktemp /tmp/zmigrate-vddk.XXXXXX)\n" +
                        "cleanup() { rm -f \"$password_file\" \"$askpass_file\" \"$source_stage\"; }\n" +
                        "trap cleanup EXIT\n" +
                        "trap 'exit 1' HUP INT TERM\n" +
                        "cat > \"$password_file\"\n" +
                        "chmod 600 \"$password_file\"\n" +
                        "printf '#!/bin/sh\\ncat %%s\\n' \"$password_file\" > \"$askpass_file\"\n" +
                        "chmod 700 \"$askpass_file\"\n" +
                        "source_uid=$(id -u)\n" +
                        "source_gid=$(id -g)\n" +
                        "cat \"$password_file\" | sudo -S -p '' cp %s \"$source_stage\"\n" +
                        "cat \"$password_file\" | sudo -S -p '' chown \"$source_uid:$source_gid\" \"$source_stage\"\n" +
                        "chmod 600 \"$source_stage\"\n" +
                        "timeout %d env DISPLAY=:0 SSH_ASKPASS_REQUIRE=force SSH_ASKPASS=\"$askpass_file\" " +
                        "setsid -w scp -P %d -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null " +
                        "\"$source_stage\" %s@%s:%s\n" +
                        "cat \"$password_file\" | timeout %d env DISPLAY=:0 SSH_ASKPASS_REQUIRE=force " +
                        "SSH_ASKPASS=\"$askpass_file\" setsid -w ssh -p %d " +
                        "-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null %s@%s %s",
                shellQuote(generationPath), VDDK_DISTRIBUTION_TIMEOUT_SECONDS,
                GATEWAY_SSH_PORT, GATEWAY_SSH_USERNAME, targetIp, uploadPath,
                VDDK_INSTALL_TIMEOUT_SECONDS, GATEWAY_SSH_PORT,
                GATEWAY_SSH_USERNAME, targetIp, shellQuote(targetInstallCommand));
    }

    private ShellResult redactPassword(ShellResult result, byte[] password) {
        String secret = new String(password, java.nio.charset.StandardCharsets.UTF_8);
        if (!secret.isEmpty()) {
            result.setStdout(redact(result.getStdout(), secret));
            result.setStderr(redact(result.getStderr(), secret));
        }
        return result;
    }

    private String redact(String value, String secret) {
        return value == null ? null : value.replace(secret, "***");
    }

    private ShellResult run(String hostname, String remoteCmd,
                            byte[] stdinData, long timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            return shellError("SSH timeout must be positive");
        }
        if (!NetworkUtils.isValidIPAddress(hostname) && !NetworkUtils.isHostname(hostname)) {
            return shellError("invalid SSH host");
        }

        String localPasswdFile = null;
        Process process = null;
        try {
            localPasswdFile = writePasswordToLocalTempFile(ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
            String sshCmd = buildSshPassCmd(localPasswdFile, GATEWAY_SSH_PORT,
                    GATEWAY_SSH_USERNAME, hostname, remoteCmd, timeoutSeconds);
            process = new ProcessBuilder("/bin/bash", "-c", sshCmd).start();

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread stdoutDrainer = drain(process.getInputStream(), stdout, "zmigrate-ssh-stdout");
            Thread stderrDrainer = drain(process.getErrorStream(), stderr, "zmigrate-ssh-stderr");

            try (OutputStream output = process.getOutputStream()) {
                if (stdinData != null) {
                    output.write(stdinData);
                    output.flush();
                }
            }

            boolean finished = process.waitFor(timeoutSeconds + 5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }
            stdoutDrainer.join(5000);
            stderrDrainer.join(5000);

            if (!finished) {
                return shellError(String.format("SSH command timed out after %d seconds", timeoutSeconds));
            }

            ShellResult result = new ShellResult();
            result.setRetCode(process.exitValue());
            result.setStdout(stdout.toString());
            result.setStderr(stderr.toString());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return shellError("interrupted while executing SSH command");
        } catch (IOException e) {
            return shellError("failed to execute SSH command: " + e.getMessage());
        } finally {
            deleteLocalTempFile(localPasswdFile);
        }
    }

    private static Thread drain(InputStream input, ByteArrayOutputStream output, String name) {
        Thread thread = new Thread(() -> {
            try (InputStream stream = input) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
            } catch (IOException e) {
                logger.debug(String.format("stopped draining %s: %s", name, e.getMessage()));
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static ShellResult shellError(String error) {
        ShellResult result = new ShellResult();
        result.setRetCode(-1);
        result.setStdout("");
        result.setStderr(error);
        return result;
    }

    public ErrorableValue<Boolean> overwriteZMigrateManagementAuthAddr() {
        ErrorableValue<String> managementIpResult = ZMigrateGatewayHelper.getGatewayManagementIp();
        if (!managementIpResult.isSuccess()) {
            return managementIpResult.error.toErrorableValue();
        }
        String managementIp = managementIpResult.result;

        // sshpass -f reads SSH password from temp file (no cmdline exposure).
        // stdin is free, so we pipe the sudo password directly to sudo -S.
        // No password appears in any command line — local or remote.
        byte[] decodedPassword = Base64.getDecoder().decode(
                ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
        try {
            String remoteCmd = String.format(
                    "sudo -S sed -i.bak 's|^auth_addr=.*|auth_addr=0.0.0.0|' %s",
                    shellQuote(GATEWAY_CONFIG_FILE_PATH));
            ShellResult rst = runSshCommandWithStdin(managementIp, remoteCmd, decodedPassword);
            if (rst.getRetCode() != 0) {
                return err(GENERIC_ERROR, "failed to overwrite ZMigrate management auth_addr")
                        .withOpaque("bash.error", rst.getStderr())
                        .withOpaque("managementIp", managementIp)
                        .logWarn(logger)
                        .toErrorableValue();
            }
            return ErrorableValue.of(true);
        } finally {
            Arrays.fill(decodedPassword, (byte) 0);
        }
    }

    /**
     * Configure systemd-timesyncd on the gateway VM with the given NTP servers.
     * Writes /etc/systemd/timesyncd.conf and restarts the service.
     *
     * @param ntpServers non-empty list of NTP server addresses (hostnames or IPs)
     * @return success or error
     */
    public ErrorableValue<Boolean> configureTimesyncd(List<String> ntpServers) {
        if (ntpServers == null || ntpServers.isEmpty()) {
            return err(INVALID_CONFIG, "ntpServers list is empty").toErrorableValue();
        }

        ErrorableValue<String> managementIpResult = ZMigrateGatewayHelper.getGatewayManagementIp();
        if (!managementIpResult.isSuccess()) {
            return managementIpResult.error.toErrorableValue();
        }
        String managementIp = managementIpResult.result;

        // Validate NTP server addresses (IPv4, IPv6, or hostname)
        for (String server : ntpServers) {
            if (!NetworkUtils.isValidIPAddress(server) && !NetworkUtils.isHostname(server)) {
                return err(GENERIC_ERROR, "invalid NTP server address: %s", server).toErrorableValue();
            }
        }

        String ntp = ntpServers.get(0);

        // Build the config content and remote command
        String remoteCmd;
        if (ntpServers.size() > 1) {
            String fallbackNtp = String.join(" ", ntpServers.subList(1, ntpServers.size()));
            remoteCmd = String.format(
                    "sudo -S sh -c \"printf '[Time]\\nNTP=%%s\\nFallbackNTP=%%s\\n' '%s' '%s' > /etc/systemd/timesyncd.conf"
                            + " && systemctl restart systemd-timesyncd\"",
                    ntp, fallbackNtp);
        } else {
            remoteCmd = String.format(
                    "sudo -S sh -c \"printf '[Time]\\nNTP=%%s\\n' '%s' > /etc/systemd/timesyncd.conf"
                            + " && systemctl restart systemd-timesyncd\"",
                    ntp);
        }

        byte[] decodedPassword = Base64.getDecoder().decode(
                ZMigrateGlobalConfig.GATEWAY_SSH_PASSWORD.value());
        try {
            ShellResult rst = runSshCommandWithStdin(managementIp, remoteCmd, decodedPassword);
            if (rst.getRetCode() != 0) {
                return ErrorableValue.ofErrorCode(err(GENERIC_ERROR,
                        "failed to configure systemd-timesyncd on gateway")
                        .withOpaque("bash.error", rst.getStderr())
                        .withOpaque("management.ip", managementIp)
                        .withOpaque("ntp.servers", String.join(",", ntpServers))
                        .logWarn(logger));
            }
            return ErrorableValue.of(true);
        } finally {
            Arrays.fill(decodedPassword, (byte) 0);
        }
    }

    public void checkZMigrateManagementIsReady(String mnIps, Completion completion) {
        ErrorableValue<String> managementIpResult = ZMigrateGatewayHelper.getGatewayManagementIp();
        if (!managementIpResult.isSuccess()) {
            completion.fail(managementIpResult.error);
            return;
        }
        String managementIp = managementIpResult.result;

        final int MAX_RETRY_TIMES = 10;
        final long RETRY_INTERVAL_SECONDS = 30;

        new AsyncTimer(TimeUnit.SECONDS, RETRY_INTERVAL_SECONDS) {
            int retryCount = 0;

            {
                __name__ = "check-zmigrate-management-ready";
            }

            @Override
            protected void execute() {
                ShellResult rst = runSshCommand(managementIp, "echo ready");
                if (rst.getRetCode() == 0) {
                    cancel();
                    completion.success();
                    return;
                }

                retryCount++;
                if (retryCount >= MAX_RETRY_TIMES) {
                    cancel();
                    completion.fail(err(GENERIC_ERROR,
                            "failed to check ZMigrate management is ready after %d retries", MAX_RETRY_TIMES)
                            .withOpaque("bash.error", rst.getStderr())
                            .withOpaque("mn.ips", mnIps)
                            .withOpaque("managementIp", managementIp)
                            .logWarn(logger));
                    return;
                }

                logger.debug(String.format("ZMigrate management not ready (attempt %d/%d), retrying in %ds",
                        retryCount, MAX_RETRY_TIMES, RETRY_INTERVAL_SECONDS));
                continueToRunThisTimer();
            }
        }.startRightNow();
    }

    public ErrorableValue<String> buildEncryptKey() {
        ErrorableValue<String> managementIpResult = ZMigrateGatewayHelper.getGatewayManagementIp();
        if (!managementIpResult.isSuccess()) {
            return managementIpResult.error.toErrorableValue();
        }
        String managementIp = managementIpResult.result;

        ShellResult rst = runSshCommand(managementIp, String.format("python3 %s", shellQuote(BUILD_ENCRYPT_KEY_FILE_PATH)));
        if (rst.getRetCode() != 0) {
            return err(GENERIC_ERROR, "failed to build encrypt key")
                    .withOpaque("bash.error", rst.getStderr())
                    .withOpaque("managementIp", managementIp)
                    .logWarn(logger)
                    .toErrorableValue();
        }
        return ErrorableValue.of(rst.getStdout().trim());
    }
}
