package org.zstack.storage.primary.nfs;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "nfs";
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
                .toService("storage.primary")
                .build();

    }
}
