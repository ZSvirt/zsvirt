package org.zstack.header.storage.primary;


public interface PrimaryStorageFactory {
    PrimaryStorageType getPrimaryStorageType();

    PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg);

    PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo);

    PrimaryStorageInventory getInventory(String uuid);

    void validateStorageProtocol(String protocol);
}
