package org.zstack.storage.backup.sftp;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "sftp";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQuerySftpBackupStorageMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("image")
                .actionsInThisPermission()
                .build();

        contributeNormalApiToOtherRole();
        apis()
                .inThisPackage()
                .toService("storage.backup")
                .build();

        apis()
                .api(
                        APIQuerySftpBackupStorageMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIReconnectSftpBackupStorageMsg.class
                )
                .toService("storage.backup.sftp")
                .build();

    }
}
