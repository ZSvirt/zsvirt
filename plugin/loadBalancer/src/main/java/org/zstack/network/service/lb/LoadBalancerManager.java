package org.zstack.network.service.lb;

import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
public interface LoadBalancerManager {
    LoadBalancerBackend getBackend(String providerType);
    LoadBalancerFactory getLoadBalancerFactory(String type);
    LoadBalancerStruct makeStruct(LoadBalancerVO vo);
    LoadBalancerServerGroupVO getDefaultServerGroup(LoadBalancerVO vo);
    LoadBalancerServerGroupVO getDefaultServerGroup(LoadBalancerListenerVO vo);
    List<String> getLoadBalancerListenterByVmNics(List<String> vmNicUuids);
    void upgradeLoadBalancerServerGroup();
    LoadBalancerFactory getLoadBalancerFactoryByApplianceVmType(String type);
}
