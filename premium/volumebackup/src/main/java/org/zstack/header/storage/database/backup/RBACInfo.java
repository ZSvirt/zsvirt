package org.zstack.header.storage.database.backup;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;

import org.zstack.header.search.SearchConstant;
@SDKPackage(packageName = "org.zstack.sdk.databasebackup")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "database-backup";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .targetResources(DatabaseBackupVO.class)
                .communityAvailable()
                .zsvProAvailable()
                .productName("disaster-recovery")
                .build();
        apis()
                .inThisPackage()
                .toService("backup.database")
                .build();

        apis()
                .api(
                        APIQueryDatabaseBackupMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
