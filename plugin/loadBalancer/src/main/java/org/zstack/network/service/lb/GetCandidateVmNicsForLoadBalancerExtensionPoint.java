package org.zstack.network.service.lb;

import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Created by heathhose on 17-5-10.
 */
public  interface GetCandidateVmNicsForLoadBalancerExtensionPoint {
    List<VmNicInventory> getCandidateVmNicsForLoadBalancerInVirtualRouter(APIGetCandidateVmNicsForLoadBalancerMsg msg, List<VmNicInventory> candidates);

}
