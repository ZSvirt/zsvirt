package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategyType;

public class MigrateVmHostAllocatorStrategyFactory extends DesignatedHostAllocatorStrategyFactory {
    private HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE, false);

    @Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
        return type;
    }
}
