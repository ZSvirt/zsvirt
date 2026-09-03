package org.zstack.twoFactorAuthentication;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "two-factor-authentication";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(org.zstack.twoFactorAuthentication.APIResetTwoFactorAuthenticationSecretMsg.class)
                .communityAvailable()
                .build();

        roleContributorBuilder()
                .roleName("identity")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("twoFactorAuthentication")
                .build();

        apis()
                .api(
                        APIQueryTwoFactorAuthenticationMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
