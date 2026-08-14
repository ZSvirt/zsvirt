package org.zstack.header.storage.primary;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "primary-storage";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryPrimaryStorageMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();

        globalReadableResourceBuilder()
                .resources(PrimaryStorageVO.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(PrimaryStorageVO.class)
                .build();
        apis()
                .inThisPackage()
                .toService("storage.primary")
                .build();

        apis()
                .api(
                        APIQueryImageCacheMsg.class,
                        APIQueryPrimaryStorageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
