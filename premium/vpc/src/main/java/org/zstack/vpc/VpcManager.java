package org.zstack.vpc;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.util.List;

public interface VpcManager {
    void createVirtualRouter(final APICreateVpcVRouterMsg msg, ReturnValueCompletion<VirtualRouterVmInventory> completion);
    List<String> getAllDnsFromVpcRouter(String vrUuid);
    String getVpcRouterDistributedRouting(String vrUuid);
}
