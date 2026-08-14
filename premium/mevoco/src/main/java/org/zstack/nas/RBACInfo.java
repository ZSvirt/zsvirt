package org.zstack.nas;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "aliyun-nas-premium";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .zsvAdvancedAvailable()
                .build();
    }
}
