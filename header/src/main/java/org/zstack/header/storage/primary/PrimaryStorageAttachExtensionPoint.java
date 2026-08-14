package org.zstack.header.storage.primary;

public interface PrimaryStorageAttachExtensionPoint {
    void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) throws PrimaryStorageException;

    void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid);

    void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid);

    void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid);
}
