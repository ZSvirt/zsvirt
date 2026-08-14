package org.zstack.core.config;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "global-config";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIQueryGlobalConfigMsg.class,
                        APIGetGlobalConfigOptionsMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("other")
                .actions(
                        APIQueryGlobalConfigMsg.class,
                        APIGetGlobalConfigOptionsMsg.class)
                .build();

        apis()
                .inThisPackage()
                .toService("globalConfig")
                .build();

        apis()
                .api(
                        APIQueryGlobalConfigMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
