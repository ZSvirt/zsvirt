package org.zstack.header.vm.hooks;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceFailToRebootHook extends VmInstanceRebootHook {
    void failToReboot(VmInstanceInventory vm, ErrorCode error);
}
