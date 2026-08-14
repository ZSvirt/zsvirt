package org.zstack.network.service.vip;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vip";
    }

    {
        permissionBuilder()
                .targetResources(VipVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("cd6ed7e009de2ed6b55d72da2e5526a2")
                .name("vip")
                .permissionBaseOnThis()
                .build();

        globalReadableResourceBuilder()
                .resources(VipVO.class)
                .build();
        apis()
                .inThisPackage()
                .toService("vip")
                .build();

        apis()
                .api(
                        APIQueryVipMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
