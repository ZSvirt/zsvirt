package org.zstack.storage.backup.imagestore;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "image-store";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIGetImagesFromImageStoreBackupStorageMsg.class,
                        APIQueryImageStoreBackupStorageMsg.class
                )
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("image")
                .actionsInThisPermission()
                .build();
        apis()
                .inThisPackage()
                .toService("storage.backup.imagestore")
                .build();

        apis()
                .api(
                        APIQueryImageStoreBackupStorageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIAddDisasterImageStoreBackupStorageMsg.class,
                        APIAddImageStoreBackupStorageMsg.class,
                        APIUpdateImageStoreBackupStorageMsg.class
                )
                .toService("storage.backup")
                .build();

    }
}
