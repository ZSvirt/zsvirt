package org.zstack.header.zone;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "zone";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryZoneMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();

        globalReadableResourceBuilder()
                .resources(ZoneVO.class)
                .build();
        apis()
                .inThisPackage()
                .toService("zone")
                .build();

        apis()
                .api(
                        APIQueryZoneMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
