package org.zstack.header.storage.primary;

public interface PrimaryStorageDetachExtensionPoint {
    void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException;

    void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid);

    void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid);

    void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid);
}
