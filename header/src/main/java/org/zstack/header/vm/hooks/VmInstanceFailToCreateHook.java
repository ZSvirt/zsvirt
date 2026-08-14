package org.zstack.header.vm.hooks;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceFailToCreateHook extends VmInstanceCreateHook {
    void failToCreate(VmInstanceInventory vm, ErrorCode error);
}
