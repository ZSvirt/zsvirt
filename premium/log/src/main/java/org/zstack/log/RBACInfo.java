package org.zstack.log;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.log.server.APIQueryLogServerMsg;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "log-configuration";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.log.**")
                .normalAPIs("org.zstack.log.server.**")
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actionsByPermissionName("log-configuration")
                .build();

        apis()
                .inThisPackage()
                .toService("log.configuration")
                .build();

        apis()
                .api(APIQueryLogServerMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
