package org.zstack.header.vo;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "core-resource";
    }

    {
        permissionBuilder()
                .normalAPIs(APIGetResourceNamesMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("identity")
                .build();

    }
}
