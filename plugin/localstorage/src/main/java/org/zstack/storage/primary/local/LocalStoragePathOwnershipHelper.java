package org.zstack.storage.primary.local;

import org.zstack.core.db.Q;
import org.zstack.storage.primary.StoragePathOwnershipHelper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Adds the LocalStorage host dimension to exact-path ownership checks. */
public final class LocalStoragePathOwnershipHelper {
    private LocalStoragePathOwnershipHelper() {
    }

    public static List<StoragePathOwnershipHelper.Owner> findOtherOwnersAtHost(
            String primaryStorageUuid, String hostUuid, String installPath,
            Collection<String> excludedResourceUuids) {
        List<StoragePathOwnershipHelper.Owner> owners = StoragePathOwnershipHelper.findOtherOwners(
                primaryStorageUuid, installPath, excludedResourceUuids);
        if (owners.isEmpty() || hostUuid == null || hostUuid.trim().isEmpty()) {
            return owners;
        }

        return owners.stream().filter(owner -> {
            String ownerHostUuid = getResourceHostUuid(primaryStorageUuid, owner.getResourceUuid());
            return ownerHostUuid == null || Objects.equals(ownerHostUuid, hostUuid);
        }).collect(Collectors.toList());
    }

    public static String getResourceHostUuid(String primaryStorageUuid, String resourceUuid) {
        if (primaryStorageUuid == null || resourceUuid == null) {
            return null;
        }
        return Q.New(LocalStorageResourceRefVO.class)
                .select(LocalStorageResourceRefVO_.hostUuid)
                .eq(LocalStorageResourceRefVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(LocalStorageResourceRefVO_.resourceUuid, resourceUuid)
                .findValue();
    }
}
