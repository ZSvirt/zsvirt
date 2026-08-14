package org.zstack.search;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "search";
    }

    {
        permissionBuilder()
                .normalAPIs(APIRefreshSearchIndexesMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("search")
                .build();

    }
}
