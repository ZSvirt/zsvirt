package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.metadata.VmInstanceMetadataConstants;
import org.zstack.header.vm.metadata.VmMetadataPathBuildExtensionPoint;
import org.zstack.header.vm.metadata.VmMetadataPathReplacementExtensionPoint;
import org.zstack.header.vm.metadata.VmMetadataResourcePersistExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageVmMetadataExtension implements VmMetadataPathBuildExtensionPoint,
        VmMetadataPathReplacementExtensionPoint, VmMetadataResourcePersistExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LocalStorageVmMetadataExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Override
    public String buildMetadataDir(String primaryStorageUuid) {
        String url = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();
        if (url == null) {
            throw new CloudRuntimeException(String.format("cannot find url for primary storage[uuid:%s]", primaryStorageUuid));
        }
        return String.format("%s/%s", normalizeBaseDir(url), VmInstanceMetadataConstants.METADATA_DIR_NAME);
    }

    @Override
    public String buildVmMetadataPath(String primaryStorageUuid, String vmInstanceUuid) {
        String url = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();
        if (url == null) {
            throw new CloudRuntimeException(String.format("cannot find url for primary storage[uuid:%s]", primaryStorageUuid));
        }
        return String.format("%s/%s/%s%s", normalizeBaseDir(url), VmInstanceMetadataConstants.METADATA_DIR_NAME, vmInstanceUuid, VmInstanceMetadataConstants.FILE_METADATA_SUFFIX);
    }

    @Override
    public PathReplacementResult calculatePathReplacements(String targetPsUuid, List<String> allOldPaths) {
        if (allOldPaths == null || allOldPaths.isEmpty()) {
            PathReplacementResult result = new PathReplacementResult();
            result.setMetadataToCurrentPathMap(Collections.emptyMap());
            return result;
        }

        String baseDir = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url).eq(PrimaryStorageVO_.uuid, targetPsUuid).findValue();
        if (baseDir == null) {
            logger.warn(String.format("LocalStorage PS[uuid:%s] has no url, path replacement disabled", targetPsUuid));
            PathReplacementResult result = new PathReplacementResult();
            result.setMetadataToCurrentPathMap(Collections.emptyMap());
            return result;
        }
        String newPrefix = normalizeBaseDir(baseDir) + "/";

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
            logger.warn(String.format("cannot extract old path prefix from any path for LocalStorage PS[uuid:%s], " +
                    "path replacement disabled", targetPsUuid));
        }

        PathReplacementResult result = new PathReplacementResult();
        result.setMetadataToCurrentPathMap(pathMap);
        result.setOldPrefix(oldPrefix);
        result.setNewPrefix(newPrefix);
        return result;
    }

    @Override
    public boolean requireHostForCleanup() {
        return true;
    }

    @Override
    public String validateMetadataPath(String primaryStorageUuid, String path) {
        if (path == null) {
            return "metadataPath cannot be null";
        }

        // Expected format: <metadataDir>/<uuid>.vmmeta
        // e.g. /local_ps/vm-metadata/a1b2c3d4e5f6...vmmeta
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

    private String normalizeBaseDir(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public void afterVolumePersist(String primaryStorageUuid, String resourceUuid,
                                   String resourceType, String hostUuid, long size, Timestamp now) {
        createResourceRef(primaryStorageUuid, resourceUuid, resourceType, hostUuid, size, now);
    }

    @Override
    public void afterSnapshotPersist(String primaryStorageUuid, String resourceUuid,
                                     String resourceType, String hostUuid, long size, Timestamp now) {
        createResourceRef(primaryStorageUuid, resourceUuid, resourceType, hostUuid, size, now);
    }

    private void createResourceRef(String primaryStorageUuid, String resourceUuid,
                                   String resourceType, String hostUuid, long size, Timestamp now) {
        boolean exists = Q.New(LocalStorageResourceRefVO.class).eq(LocalStorageResourceRefVO_.resourceUuid, resourceUuid).isExists();
        if (exists) {
            logger.debug(String.format("LocalStorageResourceRefVO for resource[uuid:%s] already exists, skip creation", resourceUuid));
            return;
        }

        LocalStorageResourceRefVO ref = new LocalStorageResourceRefVO();
        ref.setPrimaryStorageUuid(primaryStorageUuid);
        ref.setResourceUuid(resourceUuid);
        ref.setResourceType(resourceType);
        ref.setHostUuid(hostUuid);
        ref.setSize(size);
        ref.setCreateDate(now);
        ref.setLastOpDate(now);
        dbf.persist(ref);
    }

    @Override
    public void afterRegistrationRollback(List<String> resourceUuids) {
        if (resourceUuids == null || resourceUuids.isEmpty()) {
            return;
        }
        SQL.New(LocalStorageResourceRefVO.class)
                .in(LocalStorageResourceRefVO_.resourceUuid, resourceUuids)
                .hardDelete();
    }
}
