package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstancePreDestroyHook extends VmInstanceDestroyHook {
    void preDestroy(VmInstanceInventory vm);
}
