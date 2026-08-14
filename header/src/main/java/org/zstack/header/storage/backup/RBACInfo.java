package org.zstack.header.storage.backup;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.image.ImageVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "storage-backup";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIQueryBackupStorageMsg.class,
                        APIExportImageFromBackupStorageMsg.class,
                        APIDeleteExportedImageFromBackupStorageMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .targetResources(ImageVO.class)
                .build();

        roleContributorBuilder()
                .roleName("image")
                .actions(APIDeleteExportedImageFromBackupStorageMsg.class, APIExportImageFromBackupStorageMsg.class)
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(APIQueryBackupStorageMsg.class)
                .build();

        globalReadableResourceBuilder()
                .resources(BackupStorageVO.class)
                .build();

        attributeSupportResourceBuilder()
                .resources(BackupStorageVO.class)
                .build();
        apis()
                .inThisPackage()
                .toService("storage.backup")
                .build();

        apis()
                .api(
                        APIQueryBackupStorageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
