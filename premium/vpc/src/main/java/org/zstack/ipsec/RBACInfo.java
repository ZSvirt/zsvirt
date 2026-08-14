package org.zstack.ipsec;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "ipsec";
    }

    {
        permissionBuilder()
                .targetResources(IPsecConnectionVO.class)
                .zsvAdvancedAvailable()
                .build();

        roleBuilder()
                .uuid("fd5a3c75d69e40de99f94bfa0af831e0")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("ipsec")
                .build();

        apis()
                .api(
                        APIQueryIPSecConnectionMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
