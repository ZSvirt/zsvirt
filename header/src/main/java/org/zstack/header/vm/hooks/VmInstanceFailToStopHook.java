package org.zstack.header.vm.hooks;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceFailToStopHook extends VmInstanceStopHook {
    void failToStop(VmInstanceInventory vm, ErrorCode error);
}
