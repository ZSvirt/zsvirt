package org.zstack.accessKey;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "access-key";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService(AccessKeyConstant.SERVICE_ID)
                .build();

        apis()
                .api(
                        APIQueryAccessKeyMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
