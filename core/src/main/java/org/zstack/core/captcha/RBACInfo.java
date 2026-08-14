package org.zstack.core.captcha;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.role.RoleVO;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "identity-captcha-refresh";
    }

    {
        permissionBuilder()
                .targetResources(AccountVO.class, RoleVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("identity")
                .actionsByPermissionName("identity-captcha-refresh")
                .build();

        contributeNormalApiToOtherRole();

        apis()
                .inThisPackage()
                .toService("captcha")
                .build();
    }
}
