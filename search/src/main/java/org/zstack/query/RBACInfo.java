package org.zstack.query;

import org.zstack.header.description.PackageDescription;
import org.zstack.search.APIRefreshSearchIndexesMsg;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "query";
    }

    {
        permissionBuilder()
                .normalAPIs(APIBatchQueryMsg.class, APIZQLQueryMsg.class, APIRefreshSearchIndexesMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
