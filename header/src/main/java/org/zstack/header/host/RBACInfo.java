package org.zstack.header.host;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "host";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryHostMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("other")
                .actions(APIQueryHostMsg.class)
                .build();

        globalReadableResourceBuilder()
                .resources(HostVO.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(HostVO.class)
                .build();
        apis()
                .inThisPackage()
                .toService("host")
                .build();

        apis()
                .api(
                        APIGetHostTaskMsg.class
                )
                .toService("core")
                .build();

        apis()
                .api(
                        APIQueryHostMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
