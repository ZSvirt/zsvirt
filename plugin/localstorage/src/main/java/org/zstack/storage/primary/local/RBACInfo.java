package org.zstack.storage.primary.local;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.volume.VolumeVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "local-storage";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APILocalStorageGetVolumeMigratableHostsMsg.class, APILocalStorageMigrateVolumeMsg.class,
                        APIQueryLocalStorageResourceRefMsg.class)
                .targetResources(VolumeVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("other")
                .actions(APILocalStorageGetVolumeMigratableHostsMsg.class, APILocalStorageMigrateVolumeMsg.class,
                        APIQueryLocalStorageResourceRefMsg.class)
                .build();
        apis()
                .inThisPackage()
                .toService("storage.primary")
                .build();

        apis()
                .api(
                        APIQueryLocalStorageResourceRefMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
