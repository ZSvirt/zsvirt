package org.zstack.storage.primary.sharedblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SharedBlockVolumeSnapshotLanFreeLayoutValidator {
    static final int MAX_SNAPSHOT_UUIDS_PER_REQUEST = 100;

    private SharedBlockVolumeSnapshotLanFreeLayoutValidator() {
    }

    static List<String> validateRequest(List<String> snapshotUuids) {
        if (snapshotUuids == null || snapshotUuids.isEmpty()) {
            throw new IllegalArgumentException(
                    "snapshotUuids must be non-empty");
        }
        if (snapshotUuids.size() > MAX_SNAPSHOT_UUIDS_PER_REQUEST) {
            throw new IllegalArgumentException(String.format(
                    "snapshotUuids can contain at most %s values",
                    MAX_SNAPSHOT_UUIDS_PER_REQUEST));
        }

        Set<String> uniqueUuids = new HashSet<>();
        for (String snapshotUuid : snapshotUuids) {
            if (snapshotUuid == null) {
                throw new IllegalArgumentException(
                        "snapshotUuids cannot contain null");
            }
            if (isBlank(snapshotUuid)) {
                throw new IllegalArgumentException(
                        "snapshotUuids cannot contain an empty value");
            }
            if (!uniqueUuids.add(snapshotUuid)) {
                throw new IllegalArgumentException(String.format(
                        "duplicate snapshotUuid[%s]", snapshotUuid));
            }
        }
        return snapshotUuids;
    }

    static List<String> findInaccessibleSnapshots(
            List<String> snapshotUuids, List<String> accessibleSnapshotUuids) {
        if (accessibleSnapshotUuids == null) {
            return Collections.emptyList();
        }

        Set<String> accessible = new HashSet<>(accessibleSnapshotUuids);
        List<String> inaccessible = new ArrayList<>();
        for (String snapshotUuid : snapshotUuids) {
            if (!accessible.contains(snapshotUuid)) {
                inaccessible.add(snapshotUuid);
            }
        }
        return inaccessible;
    }

    static void validateResolvedTargets(
            List<SharedBlockVolumeSnapshotLanFreeTarget> targets,
            Map<String, String> primaryStorageTypes) {
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException(
                    "resolved Volume Snapshot LAN-free targets must be non-empty");
        }
        if (primaryStorageTypes == null) {
            throw new IllegalArgumentException(
                    "primary storage types cannot be null");
        }

        for (SharedBlockVolumeSnapshotLanFreeTarget target : targets) {
            if (target == null || isBlank(target.getVolumeSnapshotUuid())
                    || isBlank(target.getVolumeUuid())
                    || isBlank(target.getPrimaryStorageUuid())
                    || target.getRequestIndex() == null) {
                throw new IllegalArgumentException(
                        "invalid resolved Volume Snapshot LAN-free target");
            }
            String psType = primaryStorageTypes.get(
                    target.getPrimaryStorageUuid());
            if (!SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE.equals(
                    psType)) {
                throw new IllegalArgumentException(String.format(
                        "snapshot[%s] primary storage[%s] is not SharedBlock",
                        target.getVolumeSnapshotUuid(),
                        target.getPrimaryStorageUuid()));
            }
            validateInstallPath(
                    target.getVolumeSnapshotUuid(),
                    target.getPrimaryStorageUuid(),
                    target.getVolumeSnapshotInstallPath());
        }
    }

    private static void validateInstallPath(
            String snapshotUuid, String primaryStorageUuid, String installPath) {
        String prefix = String.format(
                "sharedblock://%s/", primaryStorageUuid);
        if (isBlank(installPath) || !installPath.startsWith(prefix)) {
            throw new IllegalArgumentException(String.format(
                    "snapshot[%s] install path[%s] does not belong to "
                            + "SharedBlock primary storage[%s]",
                    snapshotUuid, installPath, primaryStorageUuid));
        }
        String lvName = installPath.substring(prefix.length());
        if (isBlank(lvName) || lvName.contains("/")) {
            throw new IllegalArgumentException(String.format(
                    "snapshot[%s] has invalid SharedBlock LV install path[%s]",
                    snapshotUuid, installPath));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
