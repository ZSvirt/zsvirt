package org.zstack.storage.primary.sharedblock;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "shared-block";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIQuerySharedBlockMsg.class
                )
                .communityAvailable()
                .zsvProAvailable()
                .build();

        contributeNormalApiToOtherRole();

        apis()
                .inThisPackage()
                .toService("storage.primary")
                .build();

        apis()
                .api(
                        APIQuerySharedBlockGroupPrimaryStorageHostRefMsg.class,
                        APIQuerySharedBlockGroupPrimaryStorageMsg.class,
                        APIQuerySharedBlockMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIGetSharedBlockCandidateMsg.class
                )
                .toService("sharedblock")
                .build();

    }
}
