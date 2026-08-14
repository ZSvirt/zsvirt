package org.zstack.header.vpc;

import org.zstack.header.description.PackageDescription;

import org.zstack.header.search.SearchConstant;
import org.zstack.header.vpc.ha.APIQueryVpcHaGroupMsg;
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vpc-ha";
    }

    {
        permissionBuilder()
                .zsvAdvancedAvailable()
                .build();

        roleContributorBuilder()
                .roleName("vpc")
                .actionsInThisPermission()
                .build();

        apis()
                .inPackage("org.zstack.header.vpc.ha")
                .toService("vpcHa")
                .build();
        apis()
                .api(APIQueryVpcHaGroupMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
