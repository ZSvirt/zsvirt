package org.zstack.header.network.l2;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "l2";
    }

    {
        permissionBuilder()
                .targetResources(L2NetworkVO.class)
                .adminOnlyForAll()
                .normalAPIs(
                        APIUpdateL2NetworkMsg.class,
                        APIGetL2NetworkTypesMsg.class,
                        APIGetVSwitchTypesMsg.class,
                        APIQueryL2NetworkMsg.class,
                        APIQueryL2VlanNetworkMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("networks")
                .actionsInThisPermission()
                .build();

        globalReadableResourceBuilder()
                .resources(L2NetworkVO.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(L2NetworkVO.class)
                .build();

        apis()
                .inThisPackage()
                .toService("network.l2")
                .build();

        apis()
                .api(
                        APIQueryL2NetworkMsg.class,
                        APIQueryL2VlanNetworkMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
