package org.zstack.header.storage.primary;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;

import java.util.List;
import java.util.Map;

public interface PrimaryStorageAllocatorChain {
    void setNextChain(PrimaryStorageAllocatorChain next);

    PrimaryStorageAllocatorChain getNextChain();

    PrimaryStorageInventory allocate(List<PrimaryStorageVO> candidates, HostInventory candidateHost, DiskOfferingInventory diskOffering, Map<String, Object> userData) throws CloudNoAvailablePrimaryStorageException;
}
