package org.zstack.core.errorcode;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "core-error-code";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inThisPackage()
                .toService("errorcode")
                .build();
    }
}
