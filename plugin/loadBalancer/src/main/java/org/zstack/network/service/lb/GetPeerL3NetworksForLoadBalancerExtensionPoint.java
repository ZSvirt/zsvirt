package org.zstack.network.service.lb;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2019-12-26
 **/
public interface GetPeerL3NetworksForLoadBalancerExtensionPoint {
    List<L3NetworkInventory> getPeerL3NetworksForLoadBalancer(String lbUuid, List<L3NetworkInventory> candidates);
}
