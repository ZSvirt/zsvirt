package org.zstack.utils.ssh;

import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Creates or reads an ED25519 key pair at caller-supplied paths.
 * Key material is not written to the log.
 */
public class Ed25519KeyPairs {
    private Ed25519KeyPairs() {
    }

    public static String ensure(File privateKeyFile, File publicKeyFile) throws IOException {
        String existing = readPublicKey(publicKeyFile);
        if (sameKeyMaterial(existing, publicKeyFromPrivate(privateKeyFile))) {
            return existing;
        }

        File parent = privateKeyFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException(String.format("failed to create directory %s", parent.getAbsolutePath()));
        }
        if (publicKeyFile.getParentFile() != null && !publicKeyFile.getParentFile().exists()
                && !publicKeyFile.getParentFile().mkdirs()) {
            throw new IOException(String.format("failed to create directory %s",
                    publicKeyFile.getParentFile().getAbsolutePath()));
        }

        File generatedPublicKey = new File(privateKeyFile.getAbsolutePath() + ".pub");
        Files.deleteIfExists(privateKeyFile.toPath());
        Files.deleteIfExists(publicKeyFile.toPath());
        Files.deleteIfExists(generatedPublicKey.toPath());

        String command = "ssh-keygen -q -t ed25519 -N '' -C zstack -f " + shellQuote(privateKeyFile.getAbsolutePath());
        ShellResult result = runSshKeygen(command);
        if (result.getRetCode() != 0) {
            String stderr = result.getStderr() == null ? "" : result.getStderr().trim();
            throw new IOException(String.format("failed to generate ED25519 key pair, returnCode=%s, stderr=%s",
                    result.getRetCode(), stderr));
        }

        if (!generatedPublicKey.getCanonicalPath().equals(publicKeyFile.getCanonicalPath())) {
            Files.move(generatedPublicKey.toPath(), publicKeyFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        chmod(privateKeyFile, "rw-------");
        chmod(publicKeyFile, "rw-r--r--");

        String created = readPublicKey(publicKeyFile);
        if (created == null) {
            throw new IOException(String.format("ED25519 public key was not written to %s",
                    publicKeyFile.getAbsolutePath()));
        }
        return created;
    }

    private static String publicKeyFromPrivate(File privateKeyFile) throws IOException {
        if (privateKeyFile == null || !privateKeyFile.isFile() || privateKeyFile.length() == 0) {
            return null;
        }
        String command = "ssh-keygen -y -P '' -f " + shellQuote(privateKeyFile.getAbsolutePath()) + " < /dev/null";
        ShellResult result = runSshKeygen(command);
        if (result.getRetCode() != 0) {
            return null;
        }
        return firstLine(result.getStdout());
    }

    private static boolean sameKeyMaterial(String left, String right) {
        String leftMaterial = keyMaterial(left);
        String rightMaterial = keyMaterial(right);
        return leftMaterial != null && leftMaterial.equals(rightMaterial);
    }

    private static String keyMaterial(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2 || !"ssh-ed25519".equals(parts[0]) || parts[1].isEmpty()) {
            return null;
        }
        return parts[0] + " " + parts[1];
    }

    private static String firstLine(String text) {
        if (text == null) {
            return null;
        }
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    private static ShellResult runSshKeygen(String command) throws IOException {
        try {
            ShellUtils.ShellRunner runner = new ShellUtils.ShellRunner();
            runner.setCommand(command);
            runner.setWithSudo(false);
            runner.setSuppressTraceLog(true);
            return runner.run();
        } catch (ShellUtils.ShellException e) {
            throw new IOException("failed to run ssh-keygen: " + e.getMessage(), e);
        }
    }

    private static String readPublicKey(File publicKeyFile) throws IOException {
        if (publicKeyFile == null || !publicKeyFile.isFile() || publicKeyFile.length() == 0) {
            return null;
        }
        for (String line : Files.readAllLines(publicKeyFile.toPath(), StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void chmod(File file, String perms) {
        try {
            PathUtil.setFilePosixPermissions(file, perms);
        } catch (UnsupportedOperationException ignored) {
            // POSIX permissions are not available on this filesystem.
        }
    }
}
