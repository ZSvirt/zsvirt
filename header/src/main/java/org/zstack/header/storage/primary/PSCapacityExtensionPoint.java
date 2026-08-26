package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PSCapacityExtensionPoint {
    String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv);

    default String getHostUuidFromAllocatedInstallUrl(String allocatedInstallUrl) {
        return null;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid);

    @Transactional(propagation = Propagation.MANDATORY)
    void releaseCapacity(String allocatedInstallUrl, long size, String psUuid);

    PrimaryStorageType getPrimaryStorageType();
}
