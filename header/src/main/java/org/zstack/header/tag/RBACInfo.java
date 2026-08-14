package org.zstack.header.tag;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.vo.ResourceVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "tag";
    }

    {
        permissionBuilder()
                .targetResources(ResourceVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("tag")
                .build();

        apis()
                .api(
                        APIQuerySystemTagMsg.class,
                        APIQueryUserTagMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
