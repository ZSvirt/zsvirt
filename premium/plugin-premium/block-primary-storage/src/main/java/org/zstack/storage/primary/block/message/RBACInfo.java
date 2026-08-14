package org.zstack.storage.primary.block.message;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/11/7 17:27
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "block-primary-storage";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
        apis()
                .inThisPackage()
                .toService("storage.primary")
                .build();

        apis()
                .api(
                        APIGetBlockPrimaryStorageMetadataMsg.class
                )
                .toService("blockStorage")
                .build();

        apis()
                .api(
                        APIQueryBlockPrimaryStorageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
