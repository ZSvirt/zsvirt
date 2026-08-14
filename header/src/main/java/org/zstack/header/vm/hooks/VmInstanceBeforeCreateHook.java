package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceBeforeCreateHook extends VmInstanceCreateHook {
    void beforeCreate(VmInstanceInventory vm);
}
