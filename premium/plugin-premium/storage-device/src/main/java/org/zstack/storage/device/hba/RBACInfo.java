package org.zstack.storage.device.hba;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "hba";
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
