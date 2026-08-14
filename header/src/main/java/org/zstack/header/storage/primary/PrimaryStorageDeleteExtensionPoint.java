package org.zstack.header.storage.primary;

public interface PrimaryStorageDeleteExtensionPoint {
    void preDeletePrimaryStorage(PrimaryStorageInventory inv) throws PrimaryStorageException;

    void beforeDeletePrimaryStorage(PrimaryStorageInventory inv);

    void afterDeletePrimaryStorage(PrimaryStorageInventory inv);
}
