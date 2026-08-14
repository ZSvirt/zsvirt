package org.zstack.header.storage.backup;

public interface BackupStorageAttachExtensionPoint {
    String preAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void beforeAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void failToAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void afterAttachBackupStorage(BackupStorageInventory inventory, String zoneUuid);
}
