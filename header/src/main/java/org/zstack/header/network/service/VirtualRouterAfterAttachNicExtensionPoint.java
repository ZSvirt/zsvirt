package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmNicInventory;

/**
 * Created by shixin.ruan on 1/25/2018.
 */
public interface VirtualRouterAfterAttachNicExtensionPoint {
    void afterAttachNic(VmNicInventory nic, Completion completion);
    void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion);
}
