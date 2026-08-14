package org.zstack.header.vm;

/**
 * Created by xing5 on 2016/8/31.
 */
public interface VmStateChangedExtensionPoint {
    void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState);
}
