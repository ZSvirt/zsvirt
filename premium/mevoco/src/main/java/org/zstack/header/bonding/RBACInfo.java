package org.zstack.header.bonding;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "network-bonding";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("bonding")
                .build();

    }
}