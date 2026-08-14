package org.zstack.header.vm.hooks;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceFailToStartHook extends VmInstanceStartHook {
    void failToStart(VmInstanceInventory vm, ErrorCode error);
}
