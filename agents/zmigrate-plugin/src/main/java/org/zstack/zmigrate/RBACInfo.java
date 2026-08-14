package org.zstack.zmigrate;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;

@SDKPackage(packageName = "org.zstack.sdk.zmigrate")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "zmigrate-plugin";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .build();

        apis()
                .inPackage("org.zstack.zmigrate.api")
                .toService(ZMigrateConstant.SERVICE_ID)
                .build();
    }
}
