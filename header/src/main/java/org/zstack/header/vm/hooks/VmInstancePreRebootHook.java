package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstancePreRebootHook extends VmInstanceRebootHook {
    void preReboot(VmInstanceInventory vm);
}
