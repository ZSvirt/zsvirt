package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "lldp";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("hostNetwork.lldp")
                .build();

        apis()
                .api(
                        APIQueryHostNetworkInterfaceLldpMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}