package org.zstack.header.core.external.service;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "external-service";
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
                .toService("externalService")
                .build();

        apis()
                .api(APIQueryExternalServiceConfigurationMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
