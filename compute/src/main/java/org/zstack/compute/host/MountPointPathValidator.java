package org.zstack.compute.host;

import org.zstack.header.apimediator.ApiMessageInterceptionException;

import java.nio.file.Paths;

import static org.zstack.core.Platform.operr;

public final class MountPointPathValidator {
    private static final String SAFE_MOUNT_POINT_PATTERN = "^[a-zA-Z0-9_\\-./]+$";

    private MountPointPathValidator() {
    }

    public static String validateAndNormalize(String mountPoint) {
        if (mountPoint == null || mountPoint.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("mount point cannot be empty"));
        }
        if (!mountPoint.startsWith("/")) {
            throw new ApiMessageInterceptionException(operr(
                    "mount point must be an absolute path (start with '/')"));
        }
        if ("/".equals(mountPoint)) {
            throw new ApiMessageInterceptionException(operr(
                    "root directory cannot be used as mount point"));
        }
        if (mountPoint.contains("//")) {
            throw new ApiMessageInterceptionException(operr(
                    "mount point must not contain consecutive '/' characters"));
        }
        if (!mountPoint.matches(SAFE_MOUNT_POINT_PATTERN)) {
            throw new ApiMessageInterceptionException(operr(
                    "mount point must match pattern '%s'. " +
                            "allowed characters: alphanumeric, '-', '_', '.', '/'. " +
                            "valid examples: /mnt/data, /volumes/drive01, /backup-2023.disk. " +
                            "shell metacharacters are rejected to prevent command injection. " +
                            "invalid value detected: '%s'",
                    SAFE_MOUNT_POINT_PATTERN, mountPoint));
        }
        for (String segment : mountPoint.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw new ApiMessageInterceptionException(operr(
                        "mount point must not contain '.' or '..' as a path segment: '%s'", mountPoint));
            }
        }
        return normalize(mountPoint);
    }

    public static String normalize(String path) {
        return Paths.get(path).normalize().toString();
    }
}
