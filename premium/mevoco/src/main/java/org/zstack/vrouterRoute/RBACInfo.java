package org.zstack.vrouterRoute;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "v-router-route";
    }

    {
        permissionBuilder()
                .zsvAdvancedAvailable()
                .build();

        roleContributorBuilder()
                .roleName("image")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("vrouterRoute")
                .build();

        apis()
                .api(
                        APIQueryVRouterRouteEntryMsg.class,
                        APIQueryVRouterRouteTableMsg.class,
                        APIQueryVirtualRouterVRouterRouteTableRefMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
