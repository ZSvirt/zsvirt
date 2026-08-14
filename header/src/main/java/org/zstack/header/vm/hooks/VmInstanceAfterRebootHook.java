package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceAfterRebootHook extends VmInstanceRebootHook {
    void afterReboot(VmInstanceInventory vm);
}
