package org.zstack.vpc;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vpc";
    }

    {
        permissionBuilder()
                .zsvAdvancedAvailable()
                .build();

        roleBuilder()
                .uuid("cf70971a496345e487a7a22130c90510")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("vpc")
                .build();

        apis()
                .api(
                        APIQueryVpcHaGroupNetworkServiceRefMsg.class,
                        APIQueryVpcRouterMsg.class,
                        APIQueryVpcSnatStateMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
