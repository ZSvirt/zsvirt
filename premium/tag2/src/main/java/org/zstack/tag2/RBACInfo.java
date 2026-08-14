package org.zstack.tag2;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.tag.TagPatternVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "tag2";
    }

    {
        permissionBuilder()
                .targetResources(TagPatternVO.class)
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .actionsInThisPermission()
                .toOtherRole()
                .build();
        apis()
                .inThisPackage()
                .toService("tag2")
                .build();

        apis()
                .api(
                        APIQueryTagMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
