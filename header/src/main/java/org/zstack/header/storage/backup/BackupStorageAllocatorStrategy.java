package org.zstack.header.storage.backup;

import java.util.List;

/**
 */
public interface BackupStorageAllocatorStrategy {
    BackupStorageInventory allocate(BackupStorageAllocationSpec spec) throws BackupStorageException;

    List<BackupStorageInventory> allocateAllCandidates(BackupStorageAllocationSpec spec) throws BackupStorageException;
}
