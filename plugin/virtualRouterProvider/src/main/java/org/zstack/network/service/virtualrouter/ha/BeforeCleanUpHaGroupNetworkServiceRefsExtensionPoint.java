package org.zstack.network.service.virtualrouter.ha;

import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by yaohua.wu on 8/28/2019.
 */
public interface BeforeCleanUpHaGroupNetworkServiceRefsExtensionPoint {
    void beforeCleanUp(VmInstanceInventory vrInv);
}
