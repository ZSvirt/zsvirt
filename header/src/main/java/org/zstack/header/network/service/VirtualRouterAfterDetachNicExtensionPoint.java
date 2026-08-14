package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmNicInventory;

/**
 * Created by yaohua.wu on 8/14/2019.
 */
public interface VirtualRouterAfterDetachNicExtensionPoint {
    void afterDetachVirtualRouterNic(VmNicInventory nic, Completion completion);
}
