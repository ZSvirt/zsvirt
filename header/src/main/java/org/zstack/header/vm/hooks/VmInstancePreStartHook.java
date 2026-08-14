package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstancePreStartHook extends VmInstanceStartHook {
    void preStart(VmInstanceInventory vm);
}
