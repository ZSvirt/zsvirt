package org.zstack.network.service.virtualrouter;

import org.zstack.header.core.Completion;

public interface AfterAcquireVirtualRouterExtensionPoint {
    void afterAcquireVirtualRouter(VirtualRouterVmInventory vr, Completion completion);
}
