package org.zstack.storage.device.localRaid;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "local-raid";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("storageDevice")
                .build();

        apis()
                .api(
                        APIQueryLocalRaidControllerMsg.class,
                        APIQueryLocalRaidPhysicalDriveMsg.class,
                        APIQueryPhysicalDriveSelfTestHistoryMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
