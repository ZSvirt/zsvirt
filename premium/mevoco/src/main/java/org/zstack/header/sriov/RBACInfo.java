package org.zstack.header.sriov;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "sriov";
    }

    {
        permissionBuilder()
                .zsvAdvancedAvailable()
                .build();

        roleBuilder()
                .uuid("f19ac66cd876472096c0e01ba2b7bac4")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("sriov")
                .build();

    }
}
