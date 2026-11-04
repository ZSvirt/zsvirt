package org.zstack.storage.primary.sharedblock;

import org.zstack.core.db.SQL;
import org.zstack.header.volume.VolumeVO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class SharedBlockVolumeAccessPolicy {
    @FunctionalInterface
    interface AccessibleResourceFilter {
        List<String> filter(Collection<String> resourceUuids, String accountUuid,
                            Class<VolumeVO> resourceType);
    }

    private SharedBlockVolumeAccessPolicy() {
    }

    static List<String> filterAccessibleResources(
            Collection<String> resourceUuids, String accountUuid,
            Class<VolumeVO> resourceType) {
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
            List<String> requestedVolumeUuids, AccessibleResourceFilter filter) {
        if (systemAdmin) {
            return Collections.emptyList();
        }
        List<String> accessible = filter.filter(
                requestedVolumeUuids, accountUuid, VolumeVO.class);
        return SharedBlockVolumeLanFreeLayoutValidator.findInaccessibleVolumes(
                requestedVolumeUuids, accessible);
    }
}
