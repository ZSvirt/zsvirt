package org.zstack.header.vm;

/**
 * Created by Qi Le on 2019-08-21
 */
public interface VmJustAfterDeleteFromDbExtensionPoint {
    void vmJustAfterDeleteFromDbExtensionPoint(VmInstanceInventory inv, String accountUuid);
}
