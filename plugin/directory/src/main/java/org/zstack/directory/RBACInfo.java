package org.zstack.directory;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
/**
 * @author shenjin
 * @date 2022/12/7 11:27
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "vm-directory";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("directory")
                .build();

        apis()
                .api(
                        APIQueryDirectoryMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
