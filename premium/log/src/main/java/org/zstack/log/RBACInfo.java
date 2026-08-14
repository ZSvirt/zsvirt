package org.zstack.log;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "log-configuration";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("log.configuration")
                .build();

    }
}
