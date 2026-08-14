package org.zstack.header.protocol;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "route-protocol";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(
                        APICreateVRouterOspfAreaMsg.class,
                        APIDeleteVRouterOspfAreaMsg.class,
                        APIUpdateVRouterOspfAreaMsg.class
                )
                .zsvAdvancedAvailable()
                .build();

        roleContributorBuilder()
                .roleName("vpc")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("routeProtocol")
                .build();

        apis()
                .api(
                        APIQueryVRouterOspfAreaMsg.class,
                        APIQueryVRouterOspfNetworkMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
