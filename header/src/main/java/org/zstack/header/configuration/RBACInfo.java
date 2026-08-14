package org.zstack.header.configuration;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "configuration";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .targetResources(InstanceOfferingVO.class, DiskOfferingVO.class)
                .normalAPIs(APIQueryDiskOfferingMsg.class, APIQueryInstanceOfferingMsg.class, APICreateDiskOfferingMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("067c4dc358e847aba47903ca4fb1c41c")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("configuration")
                .build();

        apis()
                .api(
                        APIQueryDiskOfferingMsg.class,
                        APIQueryInstanceOfferingMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
