package org.zstack.storage.ceph;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.storage.ceph.backup.APIQueryCephBackupStorageMsg;
import org.zstack.storage.ceph.primary.APIQueryCephOsdGroupMsg;
import org.zstack.storage.ceph.primary.APIQueryCephPrimaryStorageMsg;
import org.zstack.storage.ceph.primary.APIQueryCephPrimaryStoragePoolMsg;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "ceph-storage";
    }

    {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIQueryCephBackupStorageMsg.class,
                        APIQueryCephPrimaryStoragePoolMsg.class
                )
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
                .inPackage("org.zstack.storage.ceph.backup")
                .toService("storage.backup")
                .build();
        apis()
                .api(APIQueryCephBackupStorageMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.storage.ceph.primary")
                .toService("storage.primary")
                .build();
        apis()
                .api(
                        APIQueryCephOsdGroupMsg.class,
                        APIQueryCephPrimaryStorageMsg.class,
                        APIQueryCephPrimaryStoragePoolMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
