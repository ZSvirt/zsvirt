package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstancePreStopHook extends VmInstanceStopHook {
    void preStop(VmInstanceInventory vm);
}
