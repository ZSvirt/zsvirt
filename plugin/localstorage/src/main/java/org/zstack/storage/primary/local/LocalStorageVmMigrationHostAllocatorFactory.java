package org.zstack.storage.primary.local;

import org.zstack.compute.allocator.DesignatedHostAllocatorStrategyFactory;
import org.zstack.header.allocator.HostAllocatorStrategyType;

/**
 * Created by frank on 10/24/2015.
 */
public class LocalStorageVmMigrationHostAllocatorFactory extends DesignatedHostAllocatorStrategyFactory {
    private final HostAllocatorStrategyType type = new HostAllocatorStrategyType(LocalStorageConstants.LOCAL_STORAGE_MIGRATE_VM_ALLOCATOR_TYPE, false);

    @Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
        return type;
    }
}
