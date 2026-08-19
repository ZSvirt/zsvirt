package org.zstack.network.service.lb;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "load-balancer";
    }

    {
        permissionBuilder()
                .targetResources(LoadBalancerVO.class)
                .communityAvailable()
                .zsvAdvancedAvailable()
                .build();

        roleBuilder()
                .uuid("cfc42f6e27be4fcc9e93b09356074e7e")
                .permissionsByName("vip")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("loadBalancer")
                .build();

        apis()
                .api(
                        APIQueryCertificateMsg.class,
                        APIQueryLoadBalancerListenerMsg.class,
                        APIQueryLoadBalancerMsg.class,
                        APIQueryLoadBalancerServerGroupMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        expandedQuery()
                .on(VmNicInventory.class)
                .expose("loadBalancerListener")
                .then(LoadBalancerListenerVmNicRefInventory.class)
                    .named("loadBalancerListenerRef")
                    .where(VmNicInventory.class, "uuid")
                    .equals(LoadBalancerListenerVmNicRefInventory.class, "vmNicUuid")
                    .end()
                .then(LoadBalancerListenerInventory.class)
                    .named("listener")
                    .end()
                .build();

        expandedQuery()
                .on(VipInventory.class)
                .expose("loadBalancer")
                .then(LoadBalancerInventory.class)
                    .where(VipInventory.class, "uuid")
                    .equals(LoadBalancerInventory.class, "vipUuid")
                    .end()
                .build();
    }
}
