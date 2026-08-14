package org.zstack.appliancevm;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "appliance-vm";
    }

    {
        permissionBuilder()
                .targetResources(ApplianceVmVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("vrouter")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
