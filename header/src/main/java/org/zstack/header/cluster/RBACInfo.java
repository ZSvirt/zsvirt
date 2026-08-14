package org.zstack.header.cluster;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "cluster";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryClusterMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("other")
                .actions(APIQueryClusterMsg.class)
                .build();

        globalReadableResourceBuilder()
                .resources(ClusterVO.class)
                .build();

        apis()
                .inThisPackage()
                .toService("cluster")
                .build();

        apis()
                .api(
                        APIQueryClusterMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
