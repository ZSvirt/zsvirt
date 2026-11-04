package org.zstack.resourceconfig;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.vo.ResourceVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "resource-config";
    }

    {
        permissionBuilder()
                .targetResources(ResourceVO.class)
                .normalAPIs(
                        APIGetResourceConfigMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("resourceConfig")
                .build();

        apis()
                .api(
                        APIQueryResourceConfigMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
