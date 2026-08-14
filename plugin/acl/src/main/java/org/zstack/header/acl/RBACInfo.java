package org.zstack.header.acl;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
/**
 * @author: zhanyong.miao
 * @date: 2020-03-17
 **/
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "access-control-list";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid("4366a67e46cb4e78a64899458187961e")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService(AccessControlListConstants.SERVICE_ID)
                .build();

        apis()
                .api(
                        APIQueryAccessControlListMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
