package org.zstack.storage.primary.sharedblock;

import org.zstack.header.vm.metadata.VmInstanceMetadataConstants;
import org.zstack.header.vm.metadata.VmMetadataPathBuildExtensionPoint;
import org.zstack.header.vm.metadata.VmMetadataPathReplacementExtensionPoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.storage.primary.sharedblock.SharedBlockConstants.SHARED_BLOCK_INSTALL_PATH_SCHEME;

public class SharedBlockVmMetadataExtension implements VmMetadataPathBuildExtensionPoint, VmMetadataPathReplacementExtensionPoint {
    @Override
    public String getPrimaryStorageType() {
        return SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String buildMetadataDir(String primaryStorageUuid) {
        return String.format("/dev/%s", primaryStorageUuid);
    }

    @Override
    public String buildVmMetadataPath(String primaryStorageUuid, String vmInstanceUuid) {
        return String.format("/dev/%s/%s%s", primaryStorageUuid, vmInstanceUuid, VmInstanceMetadataConstants.SBLK_METADATA_LV_SUFFIX);
    }

    @Override
    public String validateMetadataPath(String primaryStorageUuid, String path) {
        if (path == null) {
            return "metadataPath cannot be null";
        }

        // Expected format: /dev/<vgUuid>/<vmUuid>_vmmeta
        String metadataDir = buildMetadataDir(primaryStorageUuid);
        String prefix = metadataDir + "/";
        String suffix = VmInstanceMetadataConstants.SBLK_METADATA_LV_SUFFIX;

        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return String.format("metadataPath[%s] does not match expected format: %s/<uuid>%s",
                    path, metadataDir, suffix);
        }

        String uuidPart = path.substring(prefix.length(), path.length() - suffix.length());
        if (!uuidPart.matches("[0-9a-fA-F]{32}")) {
            return String.format("metadataPath[%s] contains invalid uuid[%s], expected 32-char hex", path, uuidPart);
        }
        return null;
    }

    @Override
    public PathReplacementResult calculatePathReplacements(String targetPsUuid, List<String> allOldPaths) {
        String newPrefix = SHARED_BLOCK_INSTALL_PATH_SCHEME + targetPsUuid + "/";

        String oldPrefix = null;
        for (String path : allOldPaths) {
            if (path != null && path.startsWith(SHARED_BLOCK_INSTALL_PATH_SCHEME)) {
                int schemeEnd = SHARED_BLOCK_INSTALL_PATH_SCHEME.length();
                int slashAfterVg = path.indexOf('/', schemeEnd);
                if (slashAfterVg > 0) {
                    oldPrefix = path.substring(0, slashAfterVg + 1);
                    break;
                }
            }
        }

        Map<String, String> pathMap = new LinkedHashMap<>();
        if (oldPrefix != null) {
            for (String oldPath : allOldPaths) {
                if (oldPath != null && oldPath.startsWith(oldPrefix)) {
                    pathMap.put(oldPath, newPrefix + oldPath.substring(oldPrefix.length()));
                }
            }
        }

        PathReplacementResult result = new PathReplacementResult();
        result.setMetadataToCurrentPathMap(pathMap);
        // oldPrefix/newPrefix remain as sharedblock:// scheme paths;
        // the SharedBlock storage plugin converts them to /dev/ absolute paths internally.
        result.setOldPrefix(oldPrefix);
        result.setNewPrefix(newPrefix);
        return result;
    }
}
