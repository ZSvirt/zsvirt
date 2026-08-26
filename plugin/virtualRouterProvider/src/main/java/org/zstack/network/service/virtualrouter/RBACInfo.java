package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefInventory;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipInventory;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vrouter";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("74a27f7f461e4601877c2728c52ec9e5")
                .permissionBaseOnThis()
                .permissionsByName("vip")
                .build();
        apis()
                .inThisPackage()
                .toService("virtualRouter")
                .build();

        apis()
                .api(
                        APICreateVirtualRouterOfferingMsg.class
                )
                .toService("configuration")
                .build();

        apis()
                .api(
                        APIQueryVirtualRouterOfferingMsg.class,
                        APIQueryVirtualRouterVmMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIProvisionVirtualRouterConfigMsg.class,
                        APIReconnectVirtualRouterMsg.class,
                        APIUpdateVirtualRouterMsg.class
                )
                .toService("vmInstance")
                .build();

        expandedQuery()
                .on(ApplianceVmInventory.class)
                .expose("eip")
                .then(VirtualRouterEipRefInventory.class)
                    .named("virtualRouterEipRef")
                    .where(ApplianceVmInventory.class, "uuid")
                    .equals(VirtualRouterEipRefInventory.class, "virtualRouterVmUuid")
                    .end()
                .then(EipInventory.class)
                    .named("eip")
                    .end()
                .build();

        expandedQuery()
                .on(ApplianceVmInventory.class)
                .expose("vip")
                .then(VirtualRouterVipInventory.class)
                    .named("virtualRouterVipRef")
                    .where(ApplianceVmInventory.class, "uuid")
                    .equals(VirtualRouterVipInventory.class, "virtualRouterVmUuid")
                    .end()
                .then(VipInventory.class)
                    .named("vip")
                    .end()
                .build();

        expandedQuery()
                .on(ApplianceVmInventory.class)
                .expose("portForwarding")
                .then(VirtualRouterPortForwardingRuleRefInventory.class)
                    .named("virtualRouterPortforwardingRef")
                    .where(ApplianceVmInventory.class, "uuid")
                    .equals(VirtualRouterPortForwardingRuleRefInventory.class, "virtualRouterVmUuid")
                    .end()
                .then(PortForwardingRuleInventory.class)
                    .named("portForwarding")
                    .end()
                .build();

        expandedQuery()
                .on(ApplianceVmInventory.class)
                .expose("virtualRouterOffering")
                .then(VirtualRouterOfferingInventory.class)
                    .where(ApplianceVmInventory.class, "instanceOfferingUuid")
                    .equals(VirtualRouterOfferingInventory.class, "uuid")
                    .suppress(InstanceOfferingInventory.class)
                    .end()
                .build();
    }
}
