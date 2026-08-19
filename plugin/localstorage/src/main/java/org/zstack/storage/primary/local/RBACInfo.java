package org.zstack.storage.primary.local;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;

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

        expandedQuery()
                .on(VolumeInventory.class)
                .expose("localStorageHostRef")
                .then(LocalStorageResourceRefInventory.class)
                    .where(VolumeInventory.class, "uuid")
                    .equals(LocalStorageResourceRefInventory.class, "resourceUuid")
                    .end()
                .build();

        expandedQuery()
                .on(VolumeSnapshotInventory.class)
                .expose("localStorageHostRef")
                .then(LocalStorageResourceRefInventory.class)
                    .where(VolumeSnapshotInventory.class, "uuid")
                    .equals(LocalStorageResourceRefInventory.class, "resourceUuid")
                    .end()
                .build();
    }
}
