package org.zstack.network.service.lb;

import org.zstack.header.vm.VmNicVO;

import java.util.List;

public interface LoadBalancerFactory {
    String getType();

    LoadBalancerVO persistLoadBalancer(APICreateLoadBalancerMsg msg);
    void deleteLoadBalancer(LoadBalancerVO vo);

    String getNetworkServiceType();

    String getApplianceVmType();

    LoadBalancerBackend getLoadBalancerBackend(LoadBalancerVO vo);

    String getProviderTypeByVmNicUuid(String nicUuid);

    List<VmNicVO> getAttachableVmNicsForServerGroup(LoadBalancerVO lbVO, LoadBalancerServerGroupVO groupVO);
}
