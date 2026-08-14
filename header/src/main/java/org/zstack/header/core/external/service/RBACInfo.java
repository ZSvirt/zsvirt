package org.zstack.header.core.external.service;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "core-external-service";
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
                .toService("externalService")
                .build();

    }
}
