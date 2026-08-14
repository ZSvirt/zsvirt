package org.zstack.network.service.virtualrouter.ha;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmInstanceInventory;

public interface VirtualRouterHaGroupCleanupExtensionPoint {
    void afterDeleteAllVirtualRouter(VmInstanceInventory vrInv, Completion completion);
}
