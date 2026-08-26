package org.zstack.network.service.eip;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "eip";
    }

    {
        permissionBuilder()
                .targetResources(EipVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("ecae3a96ee1b47c2aa2baee1e1110550")
                .permissionBaseOnThis()
                .permissionsByName("vip")
                .build();
        apis()
                .inThisPackage()
                .toService("eip")
                .build();

        apis()
                .api(
                        APIQueryEipMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        expandedQuery()
                .on(VmNicInventory.class)
                .expose("eip")
                .then(EipInventory.class)
                    .where(VmNicInventory.class, "uuid")
                    .equals(EipInventory.class, "vmNicUuid")
                    .end()
                .build();

        expandedQuery()
                .on(VipInventory.class)
                .expose("eip")
                .then(EipInventory.class)
                    .where(VipInventory.class, "uuid")
                    .equals(EipInventory.class, "vipUuid")
                    .end()
                .build();
    }
}
