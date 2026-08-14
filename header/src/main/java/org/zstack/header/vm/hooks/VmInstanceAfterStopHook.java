package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceAfterStopHook extends VmInstanceStopHook {
    void afterStop(VmInstanceInventory vm);
}
