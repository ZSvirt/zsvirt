package org.zstack.network.service.virtualrouter;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

/**
 * Created by xing5 on 2016/11/2.
 */
public interface VirtualRouterOfferingSelector {
    VirtualRouterOfferingInventory selectVirtualRouterOffering(L3NetworkInventory l3, List<VirtualRouterOfferingInventory> candidates);
}
