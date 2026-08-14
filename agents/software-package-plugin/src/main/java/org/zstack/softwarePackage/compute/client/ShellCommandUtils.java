package org.zstack.softwarePackage.compute.client;

import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.zstack.core.Platform.err;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.INVALID_INSTALL_PATH;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.SHELL_ERRORS;

public class ShellCommandUtils {
    private static final CLogger logger = Utils.getLogger(ShellCommandUtils.class);
    private static final String PACKAGE_INSTALL_DIRECTORY_MODE = "0755";
    private static final String PACKAGE_INSTALL_UMASK = "022";

    private static final String[] BLACKLISTED_DIRECTORIES = {
            "/etc", "/boot", "/dev", "/proc", "/sys",
            "/bin", "/sbin", "/lib", "/lib64", "/usr/bin", "/usr/sbin"
    };

    public static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        if (!path.matches("^[/a-zA-Z0-9_.-]+$")) {
            return false;
        }

        if ("/".equals(path)) {
            return false;
        }

        if (!path.startsWith("/")) {
            return false;
        }

        if (path.contains("..") || path.contains("//")) {
            return false;
        }

        for (String blacklisted : BLACKLISTED_DIRECTORIES) {
            if (path.equals(blacklisted) || path.startsWith(blacklisted + "/")) {
                return false;
            }
        }

        return true;
    }

    private static void checkPathOrThrowException(String path) {
        if (path == null || path.isEmpty()) {
            throw err(INVALID_INSTALL_PATH, "path cannot be null or empty").toException();
        }
        if (!isValidPath(path)) {
            throw err(INVALID_INSTALL_PATH, "invalid path: %s", path).toException();
        }
    }

    public static void executeCommand(String cmd) {
        ShellResult res = ShellUtils.runAndReturn(cmd, true);
        if (res.getRetCode() != 0) {
            throw err(SHELL_ERRORS, "shell command failed")
                    .withOpaque(res)
                    .logError(logger)
                    .toException();
        }
    }

    public static boolean pathExists(String path) {
        checkPathOrThrowException(path);
        String cmd = String.format("timeout 10 ls '%s'", path);
        ShellResult res = ShellUtils.runAndReturn(cmd, true);
        return res.getRetCode() == 0;
    }

    public static void writeToFile(String content, String filePath) {
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String cmd = String.format("sh -c 'echo %s | sudo base64 -d > %s'", base64Content, filePath);
        executeCommand(cmd);
    }

    public static String readFile(String filePath) {
        String cmd = String.format("sudo cat '%s'", filePath);
        ShellResult res = ShellUtils.runAndReturn(cmd, true);
        if (res.getRetCode() != 0) {
            throw err(SHELL_ERRORS, "failed to read file: %s", filePath).withOpaque(res).toException();
        }
        return res.getStdout();
    }

    public static void createDirectory(String path) {
        checkPathOrThrowException(path);
        executeCommand(String.format("mkdir -p '%s'", path));
    }

    public static void createPackageInstallDirectory(String path) {
        checkPathOrThrowException(path);
        executeCommand(String.format("install -d -m %s '%s'", PACKAGE_INSTALL_DIRECTORY_MODE, path));
    }

    public static void deleteRecursive(String path) {
        checkPathOrThrowException(path);
        executeCommand(String.format("rm -rf '%s'", path));
    }

    public static void unzipPackage(String path, String unzipPath) {
        checkPathOrThrowException(path);
        checkPathOrThrowException(unzipPath);
        executeCommand(String.format("tar -xzf %s -C %s ", path, unzipPath));
    }

    public static void unzipPackageForInstall(String path, String unzipPath) {
        checkPathOrThrowException(path);
        checkPathOrThrowException(unzipPath);
        executeCommand(String.format("sh -c 'umask %s && tar --no-same-permissions -xzf %s -C %s'", PACKAGE_INSTALL_UMASK, path, unzipPath));
    }

    public static DirectoryUsage getDirectoryUsage(String path) {
        checkPathOrThrowException(path);

        String cmd = String.format("timeout 30 df -B1 '%s' | awk 'END{print $2,$4}'", path);
        ShellResult res = ShellUtils.runAndReturn(cmd, true);

        if (res.getRetCode() != 0 || res.getStdout().trim().isEmpty()) {
            throw err(INVALID_INSTALL_PATH, "filesystem stat failed").toException();
        }

        String[] parts = res.getStdout().trim().split(" ");
        if (parts.length != 2) {
            throw err(INVALID_INSTALL_PATH, "invalid df output").withOpaque(res).toException();
        }

        try {
            return new DirectoryUsage(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            throw err(INVALID_INSTALL_PATH, "invalid number format").toException();
        }
    }

    public static class DirectoryUsage {
        public final long totalBytes;
        public final long availableBytes;

        public DirectoryUsage(long totalBytes, long availableBytes) {
            this.totalBytes = totalBytes;
            this.availableBytes = availableBytes;
        }
    }
}
