package org.zstack.zops;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "zops-plugin";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inPackage("org.zstack.zops.api")
                .toService("zops")
                .build();
    }
}
