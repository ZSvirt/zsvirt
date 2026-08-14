package org.zstack.crypto.keyprovider;

import org.zstack.crypto.keyprovider.api.APIQueryKeyProviderMsg;
import org.zstack.header.description.PackageDescription;

import org.zstack.crypto.keyprovider.nkp.api.APIQueryNkpMsg;
import org.zstack.header.search.SearchConstant;
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "key-provider";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryKeyProviderMsg.class)
                .communityAvailable()
                .build();

        roleContributorBuilder()
                .actions(APIQueryKeyProviderMsg.class)
                .toOtherRole()
                .build();

        apis()
                .inPackage("org.zstack.crypto.keyprovider.api")
                .toService("keyProvider")
                .build();
        apis()
                .api(APIQueryKeyProviderMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.crypto.keyprovider.nkp.api")
                .toService("keyProvider")
                .build();
        apis()
                .api(APIQueryNkpMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
