package org.zstack.header.simulator;

import org.zstack.header.description.PackageDescription;

/**
 * Created by kayo on 2018/7/10.
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "simulator";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }
}
