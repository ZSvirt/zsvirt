package org.zstack.network.service.virtualrouter;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

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

    }
}
