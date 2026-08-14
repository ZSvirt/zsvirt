package org.zstack.crypto.keyprovider.kms;

import org.zstack.header.description.PackageDescription;

import org.zstack.crypto.keyprovider.kms.api.APIQueryKmsMsg;
import org.zstack.header.search.SearchConstant;
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "key-provider-kms";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .build();

        apis()
                .inPackage("org.zstack.crypto.keyprovider.kms.api")
                .toService("keyProvider")
                .build();
        apis()
                .api(APIQueryKmsMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
