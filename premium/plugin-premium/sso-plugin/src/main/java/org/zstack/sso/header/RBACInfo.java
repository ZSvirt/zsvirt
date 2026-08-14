package org.zstack.sso.header;

import org.zstack.header.description.PackageDescription;

/**
 * @Author: DaoDao
 * @Date: 2022/8/23
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "sso";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIGetOAuth2TokenMsg.class)
                .communityAvailable()
                .build();

        roleContributorBuilder()
                .roleName("identity")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .actions(APIGetOAuth2TokenMsg.class)
                .toOtherRole()
                .build();
        apis()
                .inThisPackage()
                .toService("sso")
                .build();

    }
}

