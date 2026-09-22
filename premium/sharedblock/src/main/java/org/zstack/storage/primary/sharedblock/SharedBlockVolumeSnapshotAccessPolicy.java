package org.zstack.storage.primary.sharedblock;

import org.zstack.core.db.SQL;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class SharedBlockVolumeSnapshotAccessPolicy {
    @FunctionalInterface
    interface AccessibleResourceFilter {
        List<String> filter(
                Collection<String> resourceUuids, String accountUuid,
                Class<VolumeSnapshotVO> resourceType);
    }

    private SharedBlockVolumeSnapshotAccessPolicy() {
    }

    static List<String> filterAccessibleResources(
            Collection<String> resourceUuids, String accountUuid,
            Class<VolumeSnapshotVO> resourceType) {
        if (resourceUuids.isEmpty()) {
            return Collections.emptyList();
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("resourceType cannot be null");
        }

        return SQL.New(
                "select distinct ref.resourceUuid "
                        + "from AccountResourceRefVO ref "
                        + "where (ref.accountUuid = :accountUuid "
                        + "or ref.type = 'SharePublic') "
                        + "and ref.resourceUuid in (:ruuids) "
                        + "and ref.resourceType = :resourceType",
                String.class)
                .param("ruuids", resourceUuids)
                .param("accountUuid", accountUuid)
                .param("resourceType", resourceType.getSimpleName())
                .list();
    }

    static List<String> findInaccessible(
            boolean systemAdmin, String accountUuid,
            List<String> requestedSnapshotUuids,
            AccessibleResourceFilter filter) {
        if (systemAdmin) {
            return Collections.emptyList();
        }
        List<String> accessibleSnapshotUuids = filter.filter(
                requestedSnapshotUuids, accountUuid, VolumeSnapshotVO.class);
        return SharedBlockVolumeSnapshotLanFreeLayoutValidator
                .findInaccessibleSnapshots(
                        requestedSnapshotUuids, accessibleSnapshotUuids);
    }
}
