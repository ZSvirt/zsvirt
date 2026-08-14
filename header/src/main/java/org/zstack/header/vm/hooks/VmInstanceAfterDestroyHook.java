package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceAfterDestroyHook extends VmInstanceDestroyHook {
    void afterDestroy(VmInstanceInventory vm);
}
