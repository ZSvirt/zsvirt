package org.zstack.header.storage.snapshot;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.storage.snapshot.group.APIQueryVolumeSnapshotGroupMsg;
import org.zstack.header.volume.VolumeVO;

import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "snapshot";
    }

    {
        permissionBuilder()
                .targetResources(VolumeSnapshotVO.class, VolumeSnapshotTreeVO.class, VolumeVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        resourceEnsembleContributorBuilder()
                .resource(VolumeSnapshotVO.class)
                .contributeTo(VolumeVO.class)
                .build();

        roleBuilder()
                .uuid("a91363c6b4ba4e58966d17a4257668cd")
                .permissionBaseOnThis()
                .build();
        apis()
                .api(
                        APIQueryVolumeSnapshotMsg.class,
                        APIQueryVolumeSnapshotTreeMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIBackupVolumeSnapshotMsg.class,
                        APIBatchDeleteVolumeSnapshotMsg.class,
                        APIDeleteVolumeSnapshotFromBackupStorageMsg.class,
                        APIDeleteVolumeSnapshotMsg.class,
                        APIGetVolumeSnapshotSizeMsg.class,
                        APIRevertVolumeFromSnapshotMsg.class,
                        APIShrinkVolumeSnapshotMsg.class,
                        APIUpdateVolumeSnapshotMsg.class
                )
                .toService("snapshot.volume")
                .build();

        apis()
                .inPackage("org.zstack.header.storage.snapshot.group")
                .toService("snapshot.volume")
                .build();
        apis()
                .api(APIQueryVolumeSnapshotGroupMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
