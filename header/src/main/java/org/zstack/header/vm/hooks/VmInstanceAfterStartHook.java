package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceAfterStartHook extends VmInstanceStartHook {
    void afterStart(VmInstanceInventory vm);
}
