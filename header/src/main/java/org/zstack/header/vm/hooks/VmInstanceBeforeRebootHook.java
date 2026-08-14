package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceBeforeRebootHook extends VmInstanceRebootHook {
    void beforeReboot(VmInstanceInventory vm);
}
