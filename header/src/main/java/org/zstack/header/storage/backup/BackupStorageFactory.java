package org.zstack.header.storage.backup;

public interface BackupStorageFactory {
    BackupStorageType getBackupStorageType();

    BackupStorageInventory createBackupStorage(BackupStorageVO vo, APIAddBackupStorageMsg msg);

    BackupStorage getBackupStorage(BackupStorageVO vo);

    BackupStorageInventory reload(String uuid);
}
