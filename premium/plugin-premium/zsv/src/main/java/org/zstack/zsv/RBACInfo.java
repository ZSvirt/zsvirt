package org.zstack.zsv;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;

@SDKPackage(packageName = "org.zstack.sdk.zsv")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "zsv";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        apis()
                .inPackage("org.zstack.zsv.core.api")
                .toService("zsv")
                .build();
        apis()
                .inPackage("org.zstack.zsv.storage.api")
                .toService("ZsvStorage")
                .build();
    }
}
