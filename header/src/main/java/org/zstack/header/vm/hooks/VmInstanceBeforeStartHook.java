package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceBeforeStartHook extends VmInstanceStartHook {
    void beforeStart(VmInstanceInventory vm);
}
