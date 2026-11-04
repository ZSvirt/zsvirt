package org.zstack.softwarePackage.compute;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public final class SoftwarePackageUploadPath {
    private static final String HOST_STAGING_ROOT =
            "/var/lib/zstack/software-package/vm-upload";
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern TASK_UUID_PATTERN = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern VM_TARGET_PATH_PATTERN = Pattern.compile(
            "^/(root|tmp)/(?!\\.{1,2}(?:/|$))[A-Za-z0-9._-]+" +
                    "(?:/(?!\\.{1,2}$)[A-Za-z0-9._-]+)?$");

    private SoftwarePackageUploadPath() {
    }

    public static String resolveFileName(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalArgumentException("invalid software package URL", e);
        }

        String path = uri.getPath();
        String fileName = null;
        if (path != null && !path.isEmpty() && !path.endsWith("/")) {
            fileName = path.substring(path.lastIndexOf('/') + 1);
        } else if ("upload".equalsIgnoreCase(uri.getScheme())) {
            fileName = uri.getAuthority();
        }
        if (fileName == null || !FILE_NAME_PATTERN.matcher(fileName).matches() ||
                ".".equals(fileName) || "..".equals(fileName)) {
            throw new IllegalArgumentException(
                    "software package URL must contain a valid file name");
        }
        return fileName;
    }

    public static String buildHostPath(String url, String uploadTaskUuid) {
        return buildHostTaskDirectory(uploadTaskUuid) + "/" + resolveFileName(url);
    }

    public static String buildHostTaskDirectory(String uploadTaskUuid) {
        if (uploadTaskUuid == null || !TASK_UUID_PATTERN.matcher(uploadTaskUuid).matches()) {
            throw new IllegalArgumentException(
                    "upload task UUID must be a 32-character lowercase hexadecimal UUID");
        }
        return HOST_STAGING_ROOT + "/" + uploadTaskUuid;
    }

    public static String buildHostCleanupCommand(String uploadTaskUuid) {
        return "rm -rf -- '" + buildHostTaskDirectory(uploadTaskUuid) + "'";
    }

    public static boolean isValidVmTargetPath(String path) {
        return path != null && VM_TARGET_PATH_PATTERN.matcher(path).matches();
    }
}
