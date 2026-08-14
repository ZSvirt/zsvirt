package org.zstack.network.service.portforwarding;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

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

    }
}
