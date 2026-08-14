package org.zstack.header.storage.primary;

public interface PrimaryStorageChangeStateExtensionPoint {
    void preChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState nextState) throws PrimaryStorageException;

    void beforeChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState nextState);

    void afterChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState previousState);
}
