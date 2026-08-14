package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.metadata.VmInstanceMetadataConstants;
import org.zstack.header.vm.metadata.VmMetadataPathBuildExtensionPoint;
import org.zstack.header.vm.metadata.VmMetadataPathReplacementExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsVmMetadataExtension implements VmMetadataPathBuildExtensionPoint, VmMetadataPathReplacementExtensionPoint {
    private static final CLogger logger = Utils.getLogger(NfsVmMetadataExtension.class);

    @Override
    public String getPrimaryStorageType() {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
    }

    private String resolveBaseDir(String primaryStorageUuid) {
        // mountPath is the host-local mount point (e.g. /mnt/pss);
        // url is the remote NFS export (e.g. 192.168.1.1:/nfs/share) and must NOT be used
        // as a file-system path — it would generate paths the agent cannot access.
        String baseDir = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.mountPath).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();
        if (baseDir == null) {
            throw new CloudRuntimeException(String.format("cannot find mountPath for NFS primary storage[uuid:%s]", primaryStorageUuid));
        }
        return baseDir.endsWith("/") ? baseDir.substring(0, baseDir.length() - 1) : baseDir;
    }

    @Override
    public String buildMetadataDir(String primaryStorageUuid) {
        String baseDir = resolveBaseDir(primaryStorageUuid);
        return String.format("%s/%s", baseDir, VmInstanceMetadataConstants.METADATA_DIR_NAME);
    }

    @Override
    public String buildVmMetadataPath(String primaryStorageUuid, String vmInstanceUuid) {
        String baseDir = resolveBaseDir(primaryStorageUuid);
        return String.format("%s/%s/%s%s", baseDir, VmInstanceMetadataConstants.METADATA_DIR_NAME, vmInstanceUuid, VmInstanceMetadataConstants.FILE_METADATA_SUFFIX);
    }

    @Override
    public String validateMetadataPath(String primaryStorageUuid, String path) {
        if (path == null) {
            return "metadataPath cannot be null";
        }

        // Expected format: <metadataDir>/<uuid>.vmmeta
        // e.g. /mnt/pss/vm-metadata/a1b2c3d4e5f6...vmmeta
        final String metadataDir;
        try {
            metadataDir = buildMetadataDir(primaryStorageUuid);
        } catch (CloudRuntimeException e) {
            return String.format("cannot resolve metadata dir for primary storage[uuid:%s]: %s",
                    primaryStorageUuid, e.getMessage());
        }

        String prefix = metadataDir + "/";
        String suffix = VmInstanceMetadataConstants.FILE_METADATA_SUFFIX;

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
        if (allOldPaths == null || allOldPaths.isEmpty()) {
            PathReplacementResult result = new PathReplacementResult();
            result.setMetadataToCurrentPathMap(Collections.emptyMap());
            return result;
        }

        String baseDir = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.mountPath).eq(PrimaryStorageVO_.uuid, targetPsUuid).findValue();
        if (baseDir == null) {
            logger.warn(String.format("NFS PS[uuid:%s] has no mountPath, path replacement disabled", targetPsUuid));
            PathReplacementResult result = new PathReplacementResult();
            result.setMetadataToCurrentPathMap(Collections.emptyMap());
            return result;
        }
        String newPrefix = baseDir.endsWith("/") ? baseDir : baseDir + "/";

        // Extract old prefix from the first recognizable path
        String oldPrefix = null;
        for (String path : allOldPaths) {
            oldPrefix = VmInstanceMetadataConstants.extractOldPrefix(path);
            if (oldPrefix != null) break;
        }

        Map<String, String> pathMap = new LinkedHashMap<>();
        if (oldPrefix != null) {
            for (String oldPath : allOldPaths) {
                if (oldPath != null && oldPath.startsWith(oldPrefix)) {
                    pathMap.put(oldPath, newPrefix + oldPath.substring(oldPrefix.length()));
                }
            }
        } else {
            logger.warn(String.format("cannot extract old path prefix from any path for NFS PS[uuid:%s], " +
                    "path replacement disabled", targetPsUuid));
        }

        PathReplacementResult result = new PathReplacementResult();
        result.setMetadataToCurrentPathMap(pathMap);
        result.setOldPrefix(oldPrefix);
        result.setNewPrefix(newPrefix);
        return result;
    }
}
