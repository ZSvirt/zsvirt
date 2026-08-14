package org.zstack.header.console;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "console";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                    APIRequestConsoleAccessMsg.class,
                    APIQueryConsoleProxyAgentMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("6f5a7d6d2da9499da9e4bdb079f65adf")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .actions(APIQueryConsoleProxyAgentMsg.class)
                .toOtherRole()
                .build();
        apis()
                .inThisPackage()
                .toService("console")
                .build();

        apis()
                .api(
                        APIQueryConsoleProxyAgentMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
