package org.zstack.header.storage.volume.backup;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.database.backup.APISyncDatabaseBackupMsg;

import org.zstack.header.search.SearchConstant;
/**
 * Created by kayo on 2018/7/30.
 */
@SDKPackage(packageName = "org.zstack.sdk.storage.volumebackup")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "volume-backup";
    }

    {
        permissionBuilder()
                .adminOnlyAPIs(APISyncVolumeBackupMsg.class,
                        APISyncVmBackupMsg.class,
                        APISyncDatabaseBackupMsg.class)
                .targetResources(VolumeBackupVO.class)
                .communityAvailable()
                .zsvProAvailable()
                .productName("disaster-recovery")
                .build();

        roleBuilder()
                .uuid("5eecda569bc84b0ab2e89185c3cddd77")
                .permissionBaseOnThis()
                .build();
        apis()
                .inThisPackage()
                .toService("backup.volume")
                .build();

        apis()
                .api(
                        APIQueryVolumeBackupMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

    }
}
