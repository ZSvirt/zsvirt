package org.zstack.storage.primary.sharedblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SharedBlockVolumeLanFreeLayoutValidator {
    static final int MAX_VOLUME_UUIDS_PER_REQUEST = 100;

    private SharedBlockVolumeLanFreeLayoutValidator() {
    }

    static List<String> validateRequest(List<String> volumeUuids) {
        if (volumeUuids == null || volumeUuids.isEmpty()) {
            throw new IllegalArgumentException("volumeUuids must be non-empty");
        }
        if (volumeUuids.size() > MAX_VOLUME_UUIDS_PER_REQUEST) {
            throw new IllegalArgumentException("volumeUuids can contain at most 100 values");
        }
        Set<String> unique = new HashSet<>();
        for (String uuid : volumeUuids) {
            if (uuid == null) {
                throw new IllegalArgumentException("volumeUuids cannot contain null");
            }
            if (uuid.trim().isEmpty()) {
                throw new IllegalArgumentException("volumeUuids cannot contain an empty value");
            }
            if (!unique.add(uuid)) {
                throw new IllegalArgumentException("duplicate volumeUuid[" + uuid + "]");
            }
        }
        return volumeUuids;
    }

    static List<String> findInaccessibleVolumes(
            List<String> volumeUuids, List<String> accessibleVolumeUuids) {
        if (accessibleVolumeUuids == null) {
            return Collections.emptyList();
        }
        Set<String> accessible = new HashSet<>(accessibleVolumeUuids);
        List<String> inaccessible = new ArrayList<>();
        for (String uuid : volumeUuids) {
            if (!accessible.contains(uuid)) {
                inaccessible.add(uuid);
            }
        }
        return inaccessible;
    }

    static void validateResolvedTargets(
            List<SharedBlockVolumeLanFreeTarget> targets,
            Map<String, String> primaryStorageTypes) {
        if (targets == null || targets.isEmpty() || primaryStorageTypes == null) {
            throw new IllegalArgumentException("resolved Volume LAN-free targets are invalid");
        }
        for (SharedBlockVolumeLanFreeTarget target : targets) {
            if (target == null || isBlank(target.getVolumeUuid())
                    || isBlank(target.getPrimaryStorageUuid())
                    || target.getRequestIndex() == null) {
                throw new IllegalArgumentException("invalid resolved Volume LAN-free target");
            }
            if (!SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE.equals(
                    primaryStorageTypes.get(target.getPrimaryStorageUuid()))) {
                throw new IllegalArgumentException(String.format(
                        "volume[%s] primary storage[%s] is not SharedBlock",
                        target.getVolumeUuid(), target.getPrimaryStorageUuid()));
            }
            String path = target.getVolumeInstallPath();
            String prefix = "sharedblock://" + target.getPrimaryStorageUuid() + "/";
            if (isBlank(path) || !path.startsWith(prefix)) {
                throw new IllegalArgumentException(String.format(
                        "volume[%s] install path[%s] does not belong to SharedBlock primary storage[%s]",
                        target.getVolumeUuid(), path, target.getPrimaryStorageUuid()));
            }
            String lvName = path.substring(prefix.length());
            if (isBlank(lvName) || lvName.contains("/")) {
                throw new IllegalArgumentException(String.format(
                        "volume[%s] has invalid SharedBlock LV install path[%s]",
                        target.getVolumeUuid(), path));
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
