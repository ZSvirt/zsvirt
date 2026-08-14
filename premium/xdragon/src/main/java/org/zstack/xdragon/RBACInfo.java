package org.zstack.xdragon;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "x-dragon";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(APIAddXDragonHostMsg.class)
                .zsvAdvancedAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("host")
                .build();

    }
}
