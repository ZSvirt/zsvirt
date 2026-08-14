package org.zstack.guesttools.advanced;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;

import org.zstack.header.search.SearchConstant;
@SDKPackage(packageName = "org.zstack.sdk.guesttools.advanced")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "guest-tools-advanced";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("guest.tools")
                .build();

        apis()
                .api(
                        APIQueryVmCustomSpecificationMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
