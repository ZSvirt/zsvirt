package org.zstack.header.vm.hooks;

import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstanceBeforeDestroyHook extends VmInstanceDestroyHook {
    void beforeDestroy(VmInstanceInventory vm);
}
