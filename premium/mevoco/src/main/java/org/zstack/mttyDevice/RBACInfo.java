package org.zstack.mttyDevice;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "mtty-devices";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryMttyDeviceMsg.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("77affc9ac2eb452cb7170953460e9770")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("mttyDevice")
                .build();

        apis()
                .api(
                        APIQueryMttyDeviceMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
