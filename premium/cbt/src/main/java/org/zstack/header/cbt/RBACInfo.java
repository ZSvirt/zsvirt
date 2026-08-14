package org.zstack.header.cbt;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "cbt";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("cbt")
                .build();

        apis()
                .api(
                        APIQueryCbtTaskMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
