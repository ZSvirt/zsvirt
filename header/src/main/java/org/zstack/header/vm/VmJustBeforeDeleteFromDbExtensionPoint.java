package org.zstack.header.vm;

/**
 * Created by xing5 on 2017/6/26.
 */
public interface VmJustBeforeDeleteFromDbExtensionPoint {
    void vmJustBeforeDeleteFromDb(VmInstanceInventory inv);
}
