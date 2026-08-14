package org.zstack.managements.api;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.managements.header.PremiumManagementsConstant;

@SDKPackage(packageName="org.zstack.sdk.managements")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "management-node-premium";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .adminOnlyForAll()
                .build();

        apis()
                .inThisPackage()
                .toService(PremiumManagementsConstant.HA2_SERVICE_ID)
                .build();
    }
}
