package org.zstack.storage.device.fibreChannel;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "fibre-channel";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .zsvAdvancedAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("storageDevice")
                .build();

        apis()
                .api(
                        APIQueryFiberChannelLunMsg.class,
                        APIQueryFiberChannelStorageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
