package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceBeforeStopHook extends VmInstanceStopHook {
    void beforeStop(VmInstanceInventory vm);
}
