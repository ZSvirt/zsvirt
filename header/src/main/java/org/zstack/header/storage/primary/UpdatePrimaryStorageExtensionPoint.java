package org.zstack.header.storage.primary;

/**
 * Created by MaJin on 2017/11/29.
 */
public interface UpdatePrimaryStorageExtensionPoint {
    void beforeUpdatePrimaryStorage(PrimaryStorageInventory ps);

    void afterUpdatePrimaryStorage(PrimaryStorageInventory ps);
}
