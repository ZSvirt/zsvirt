package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstancePreCreateHook extends VmInstanceCreateHook {
    void preCreate(VmInstanceInventory vm);
}
