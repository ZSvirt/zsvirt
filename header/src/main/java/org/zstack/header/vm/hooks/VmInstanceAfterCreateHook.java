package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceAfterCreateHook extends VmInstanceCreateHook {
    void afterCreate(VmInstanceInventory vm);
}
