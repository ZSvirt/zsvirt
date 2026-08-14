package org.zstack.ha;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vm-ha";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("vm")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .roleName("vm-operation-without-create-permission")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("ha")
                .build();

    }
}
