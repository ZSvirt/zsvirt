package org.zstack.storage.primary;

import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves active database resources that own an exact physical storage path. */
public final class StoragePathOwnershipHelper {
    public enum ResourceType {
        VOLUME,
        SNAPSHOT
    }

    public static final class Owner {
        private final String resourceUuid;
        private final ResourceType resourceType;

        private Owner(String resourceUuid, ResourceType resourceType) {
            this.resourceUuid = resourceUuid;
            this.resourceType = resourceType;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        @Override
        public String toString() {
            return String.format("%s[uuid:%s]", resourceType, resourceUuid);
        }
    }

    private StoragePathOwnershipHelper() {
    }

    public static List<Owner> findOtherOwners(String primaryStorageUuid, String installPath,
                                               Collection<String> excludedResourceUuids) {
        if (primaryStorageUuid == null || primaryStorageUuid.trim().isEmpty()
                || installPath == null || installPath.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> excluded = excludedResourceUuids == null
                ? Collections.emptySet() : new LinkedHashSet<>(excludedResourceUuids);
        List<Owner> owners = new ArrayList<>();

        List<String> volumeUuids = Q.New(VolumeVO.class)
                .select(VolumeVO_.uuid)
                .eq(VolumeVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(VolumeVO_.installPath, installPath)
                .listValues();
        if (volumeUuids != null) {
            volumeUuids.stream()
                    .filter(uuid -> !excluded.contains(uuid))
                    .map(uuid -> new Owner(uuid, ResourceType.VOLUME))
                    .forEach(owners::add);
        }

        List<String> snapshotUuids = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.uuid)
                .eq(VolumeSnapshotVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(VolumeSnapshotVO_.primaryStorageInstallPath, installPath)
                .listValues();
        if (snapshotUuids != null) {
            snapshotUuids.stream()
                    .filter(uuid -> !excluded.contains(uuid))
                    .map(uuid -> new Owner(uuid, ResourceType.SNAPSHOT))
                    .forEach(owners::add);
        }

        return owners;
    }
}
