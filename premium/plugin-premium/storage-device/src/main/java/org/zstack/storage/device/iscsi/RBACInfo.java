package org.zstack.storage.device.iscsi;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "iscsi";
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
                        APIQueryIscsiLunMsg.class,
                        APIQueryIscsiServerMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
