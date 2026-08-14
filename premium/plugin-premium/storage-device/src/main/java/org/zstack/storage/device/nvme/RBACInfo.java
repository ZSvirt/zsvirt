package org.zstack.storage.device.nvme;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "nvme";
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
                        APIQueryNvmeLunMsg.class,
                        APIQueryNvmeServerMsg.class,
                        APIQueryNvmeTargetMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
