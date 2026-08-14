package org.zstack.header.storage.primary;

public interface PrimaryStorageAllocatorStrategyFactory {
    PrimaryStorageAllocatorStrategyType getPrimaryStorageAllocatorStrategyType();

    PrimaryStorageAllocatorStrategy getPrimaryStorageAllocatorStrategy();
}
