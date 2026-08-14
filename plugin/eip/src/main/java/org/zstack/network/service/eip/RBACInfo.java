package org.zstack.network.service.eip;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "eip";
    }

    {
        permissionBuilder()
                .targetResources(EipVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("ecae3a96ee1b47c2aa2baee1e1110550")
                .permissionBaseOnThis()
                .permissionsByName("vip")
                .build();
        apis()
                .inThisPackage()
                .toService("eip")
                .build();

        apis()
                .api(
                        APIQueryEipMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
