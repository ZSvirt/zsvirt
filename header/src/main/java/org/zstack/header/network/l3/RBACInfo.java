package org.zstack.header.network.l3;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "l3";
    }

    {
        permissionBuilder()
                .targetResources(L3NetworkVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .name("networks")
                .uuid("884b0fcc99b04120807e64466fd63336")
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .actions(
                    APIQueryL3NetworkMsg.class,
                    APIQueryIpRangeMsg.class
                )
                .toOtherRole()
                .build();

        globalReadableResourceBuilder()
                .resources(UsedIpVO.class)
                .resources(IpRangeVO.class)
                .resources(AddressPoolVO.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(L3NetworkVO.class)
                .build();

        apis()
                .inThisPackage()
                .toService("network.l3")
                .build();

        apis()
                .api(
                        APIQueryAddressPoolMsg.class,
                        APIQueryIpAddressMsg.class,
                        APIQueryIpRangeMsg.class,
                        APIQueryL3NetworkMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
