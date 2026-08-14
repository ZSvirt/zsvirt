package org.zstack.header.storage.backup;

public interface BackupStorageChangeStateExtensionPoint {
    void preChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState nextState) throws BackupStorageException;

    void beforeChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState nextState);

    void afterChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState previousState);
}
