package org.zstack.header.apimediator;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "portal";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
    }
}
