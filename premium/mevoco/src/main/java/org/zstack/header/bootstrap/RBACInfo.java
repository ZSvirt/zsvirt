package org.zstack.header.bootstrap;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "mini-bootstrap";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .build();
        apis()
                .inThisPackage()
                .toService("mevoco")
                .build();

    }
}
