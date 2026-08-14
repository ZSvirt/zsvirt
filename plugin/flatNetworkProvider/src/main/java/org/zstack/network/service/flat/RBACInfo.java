package org.zstack.network.service.flat;

import org.zstack.header.description.PackageDescription;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "flat-l3";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("networks")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIGetL3NetworkDhcpIpAddressMsg.class,
                    APIGetL3NetworkIpStatisticMsg.class
                )
                .build();
        apis()
                .inThisPackage()
                .toService("flat.dhcp")
                .build();

    }
}
