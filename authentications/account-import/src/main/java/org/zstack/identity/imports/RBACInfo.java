package org.zstack.identity.imports;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;
import org.zstack.identity.imports.api.APIQueryThirdPartyAccountSourceBindingMsg;

@SDKPackage(packageName = "org.zstack.sdk.identity.imports")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "account-imports";
    }

    {
        permissionBuilder()
                .normalAPIs(APIQueryThirdPartyAccountSourceBindingMsg.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("identity")
                .actionsInThisPermission()
                .build();

        apis()
                .inPackage("org.zstack.identity.imports.api")
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
