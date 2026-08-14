package org.zstack.header.storage.addon.primary;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "external-primary-storage";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.storage.addon.primary.**")
                .communityAvailable()
                .build();

        globalReadableResourceBuilder()
                .resources(ExternalPrimaryStorageVO.class)
                .build();
        apis()
                .inThisPackage()
                .toService("storage.primary")
                .build();

        apis()
                .api(
                        APIDiscoverExternalPrimaryStorageMsg.class
                )
                .toService("externalPrimaryStorage")
                .build();

    }
}
