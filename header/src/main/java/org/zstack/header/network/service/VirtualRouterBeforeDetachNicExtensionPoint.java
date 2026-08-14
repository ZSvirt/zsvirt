package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmNicInventory;

/**
 * Created by shixin.ruan on 1/25/2018.
 */
public interface VirtualRouterBeforeDetachNicExtensionPoint {
    void beforeDetachNic(VmNicInventory nic, Completion completion);
    void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion);
}
