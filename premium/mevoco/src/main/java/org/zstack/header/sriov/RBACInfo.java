package org.zstack.header.sriov;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.pciDevice.virtual.sr_iov.APIQueryEthernetVFMsg;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "sriov";
    }

    {
        permissionBuilder()
                .normalAPIs(APIQueryEthernetVFMsg.class)
                .zsvAdvancedAvailable()
                .build();

        roleBuilder()
                .uuid("f19ac66cd876472096c0e01ba2b7bac4")
                .actions(APIQueryEthernetVFMsg.class)
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("sriov")
                .build();
        apis()
                .api(APIQueryEthernetVFMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
