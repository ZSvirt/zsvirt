package org.zstack.header.volume;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.search.SearchConstant;

public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "volume";
    }

    {
        permissionBuilder()
                .targetResources(VolumeVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("snapshot")
                .actions(APICreateVolumeSnapshotMsg.class)
                .build();

        roleContributorBuilder()
                .actions(APIQueryVolumeMsg.class)
                .toOtherRole()
                .build();

        roleBuilder()
                .uuid("b4368d05a2394f1fb75173683f55456f")
                .permissionBaseOnThis()
                .excludeActions(APICreateVolumeSnapshotMsg.class)
                .build();
        apis()
                .api(
                        APIQueryVolumeMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIAttachDataVolumeToHostMsg.class,
                        APIAttachDataVolumeToVmMsg.class,
                        APIBatchSyncVolumeSizeMsg.class,
                        APIChangeVolumeStateMsg.class,
                        APICreateDataVolumeFromVolumeSnapshotMsg.class,
                        APICreateDataVolumeFromVolumeTemplateMsg.class,
                        APICreateDataVolumeMsg.class,
                        APICreateVolumeSnapshotGroupMsg.class,
                        APICreateVolumeSnapshotMsg.class,
                        APIDeleteDataVolumeMsg.class,
                        APIDetachDataVolumeFromHostMsg.class,
                        APIDetachDataVolumeFromVmMsg.class,
                        APIExpungeDataVolumeMsg.class,
                        APIFlattenVolumeMsg.class,
                        APIGetDataVolumeAttachableVmMsg.class,
                        APIGetVolumeCapabilitiesMsg.class,
                        APIGetVolumeFormatMsg.class,
                        APIRecoverDataVolumeMsg.class,
                        APISyncVolumeSizeMsg.class,
                        APIUndoSnapshotCreationMsg.class,
                        APIUpdateVolumeMsg.class
                )
                .toService("volume")
                .build();

    }
}
