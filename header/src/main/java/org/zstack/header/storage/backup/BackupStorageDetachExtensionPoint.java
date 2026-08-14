package org.zstack.header.storage.backup;

public interface BackupStorageDetachExtensionPoint {
    void preDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid) throws BackupStorageException;

    void beforeDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void failToDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void afterDetachBackupStorage(BackupStorageInventory inventory, String zoneUuid);
}
