package org.zstack.header.network.service;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "network-service";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIAttachNetworkServiceToL3NetworkMsg.class,
                        APIQueryNetworkServiceProviderMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("networks")
                .actions(APIQueryNetworkServiceProviderMsg.class, APIAttachNetworkServiceToL3NetworkMsg.class)
                .build();

        apis()
                .inThisPackage()
                .toService("network.service")
                .build();

        apis()
                .api(
                        APIAttachNetworkServiceToL3NetworkMsg.class,
                        APIDetachNetworkServiceFromL3NetworkMsg.class
                )
                .toService("network.l3")
                .build();

        apis()
                .api(
                        APIQueryNetworkServiceL3NetworkRefMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
