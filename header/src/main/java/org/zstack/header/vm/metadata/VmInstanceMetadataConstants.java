package org.zstack.header.vm.metadata;

public class VmInstanceMetadataConstants {
    private VmInstanceMetadataConstants() {
    }

    public static final String SBLK_METADATA_LV_SUFFIX = "_vmmeta";
    public static final String FILE_METADATA_SUFFIX = ".vmmeta";
    public static final String METADATA_DIR_NAME = "vm-metadata";

    private static final String[] STORAGE_PATH_MARKERS = {
            "/rootVolumes/", "/dataVolumes/", "/volumeSnapshots/", "/memory/"
    };

    /**
     * Extract the storage mount-point prefix from a volume install path.
     * <pre>
     *   /mnt/ps/rootVolumes/acct-xxx/vol-xxx/xxx.qcow2  →  /mnt/ps/
     *   /local_ps/dataVolumes/acct-xxx/vol-xxx/xxx.qcow2 →  /local_ps/
     * </pre>
     * Uses {@code lastIndexOf} so that mount paths which themselves contain a
     * marker string (e.g. {@code /rootVolumes/rootVolumes/…}) are handled
     * correctly — the last occurrence is always the real subdirectory boundary.
     */
    public static String extractOldPrefix(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }

        int latest = -1;
        for (String marker : STORAGE_PATH_MARKERS) {
            int idx = path.lastIndexOf(marker);
            if (idx > latest) {
                latest = idx;
            }
        }
        return latest < 0 ? null : path.substring(0, latest + 1);
    }
}