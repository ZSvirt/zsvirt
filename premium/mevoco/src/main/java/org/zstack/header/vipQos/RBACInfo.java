package org.zstack.header.vipQos;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vip-qos";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("eip")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .roleName("load-balancer")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .roleName("port-forwarding")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .roleName("ipsec")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("VipQos")
                .build();

    }
}
