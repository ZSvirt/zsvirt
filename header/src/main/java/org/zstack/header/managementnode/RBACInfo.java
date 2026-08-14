package org.zstack.header.managementnode;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

/**
 * Created by kayo on 2018/7/10.
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "management-node";
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
                .toService("managementNode")
                .build();

        apis()
                .api(
                        APIQueryManagementNodeMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
