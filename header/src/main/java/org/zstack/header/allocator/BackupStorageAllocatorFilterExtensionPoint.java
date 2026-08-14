package org.zstack.header.allocator;

import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.List;

public interface BackupStorageAllocatorFilterExtensionPoint {
    List<BackupStorageInventory> filterBackupStorageCandidatesByPS(List<BackupStorageInventory> candidates, String psUuid);
}
