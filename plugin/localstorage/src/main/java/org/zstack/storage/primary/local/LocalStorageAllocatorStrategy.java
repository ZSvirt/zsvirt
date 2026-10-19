package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategy;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.storage.primary.allocate.PrimaryStorageAllocatorChainBuilder;

import java.util.Collections;
import java.util.List;

public class LocalStorageAllocatorStrategy implements PrimaryStorageAllocatorStrategy {
    private final PrimaryStorageAllocatorChainBuilder builder;

    public LocalStorageAllocatorStrategy(PrimaryStorageAllocatorChainBuilder builder) {
        this.builder = builder;
    }

    @Override
    public PrimaryStorageInventory allocate(PrimaryStorageAllocationSpec spec) {
        return allocateAllCandidates(spec).get(0);
    }

    @Override
    public void sort(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> candidates) {
    }

    @Override
    public List<PrimaryStorageInventory> allocateAllCandidates(PrimaryStorageAllocationSpec spec) {
        List<PrimaryStorageInventory> invs = PrimaryStorageInventory.valueOf(builder.build().allocate(spec));
        Collections.shuffle(invs);
        return invs;
    }
}
