package org.zstack.header.vm.hooks;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceFailToDestroyHook extends VmInstanceDestroyHook {
    void failToDestroy(VmInstanceInventory vm, ErrorCode error);
}
