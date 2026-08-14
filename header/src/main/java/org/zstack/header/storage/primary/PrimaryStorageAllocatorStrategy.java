package org.zstack.header.storage.primary;

import java.util.List;

public interface PrimaryStorageAllocatorStrategy {
    PrimaryStorageInventory allocate(PrimaryStorageAllocationSpec spec);

    void sort(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> candidates);

    List<PrimaryStorageInventory> allocateAllCandidates(PrimaryStorageAllocationSpec spec);
}
