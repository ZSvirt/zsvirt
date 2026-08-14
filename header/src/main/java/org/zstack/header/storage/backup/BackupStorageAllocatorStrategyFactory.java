package org.zstack.header.storage.backup;

/**
 */
public interface BackupStorageAllocatorStrategyFactory {
    BackupStorageAllocatorStrategyType getType();

    BackupStorageAllocatorStrategy getAllocatorStrategy();
}
