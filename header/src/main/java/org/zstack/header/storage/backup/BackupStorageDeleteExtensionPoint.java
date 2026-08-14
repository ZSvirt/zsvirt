package org.zstack.header.storage.backup;

public interface BackupStorageDeleteExtensionPoint {
    void preDeleteSecondaryStorage(BackupStorageInventory inv) throws BackupStorageException;

    void beforeDeleteSecondaryStorage(BackupStorageInventory inv);

    void afterDeleteSecondaryStorage(BackupStorageInventory inv);
}
