package org.zstack.header.affinitygroup;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "affinity-group";
    }

    {
        permissionBuilder()
                .targetResources(AffinityGroupVO.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("4032cd1b898a4a3b93e8e83ad2bf84df")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("affinityGroup")
                .build();

        apis()
                .api(
                        APIQueryAffinityGroupMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
