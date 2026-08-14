package org.zstack.storage.backup;

import org.zstack.header.Service;
import org.zstack.header.storage.backup.BackupStorageAllocatorStrategyFactory;
import org.zstack.header.storage.backup.BackupStorageFactory;
import org.zstack.header.storage.backup.BackupStorageType;

public interface BackupStorageManager {
    BackupStorageFactory getBackupStorageFactory(BackupStorageType type);

    BackupStorageAllocatorStrategyFactory getAllocatorFactory(String type);
}
