package org.zstack.core.eventlog;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "core-event-log";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inThisPackage()
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
