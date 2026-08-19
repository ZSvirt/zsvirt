package org.zstack.network.service.portforwarding;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "port-forwarding";
    }

    {
        permissionBuilder()
                .targetResources(PortForwardingRuleVO.class)
                .communityAvailable()
                .zsvAdvancedAvailable()
                .build();

        roleBuilder()
                .uuid("62617332af7241dbadf8e0570197d42f")
                .permissionBaseOnThis()
                .permissionsByName("vip")
                .build();
        apis()
                .inThisPackage()
                .toService("portForwarding")
                .build();

        apis()
                .api(
                        APIQueryPortForwardingRuleMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        expandedQuery()
                .on(VmNicInventory.class)
                .expose("portForwarding")
                .then(PortForwardingRuleInventory.class)
                    .where(VmNicInventory.class, "uuid")
                    .equals(PortForwardingRuleInventory.class, "vmNicUuid")
                    .end()
                .build();

        expandedQuery()
                .on(VipInventory.class)
                .expose("portForwarding")
                .then(PortForwardingRuleInventory.class)
                    .where(VipInventory.class, "uuid")
                    .equals(PortForwardingRuleInventory.class, "vipUuid")
                    .end()
                .build();
    }
}
