package org.zstack.storage.ceph.primary;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.SQL;
import org.zstack.core.trash.StorageTrash;
import org.zstack.header.core.Completion;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotAfterDeleteExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.storage.ceph.CephConstants.CEPH_PRIMARY_STORAGE_TYPE;

public class CephVolumeSnapshotAfterDeleteExtension implements VolumeSnapshotAfterDeleteExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private StorageTrash trash;

    private static final CLogger logger = Utils.getLogger(CephVolumeSnapshotAfterDeleteExtension.class);

    @Override
    public void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
    }

    @Override
    public void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot) {
    }

    private String getVolumeInstallPathFromSnapshot(String snapshotInstallPath) {
        return snapshotInstallPath.split("@")[0];
    }

    private boolean isCephPs(String volumeUuid) {
        String sql = "select ps.type from PrimaryStorageVO ps, VolumeVO vol where vol.primaryStorageUuid = ps.uuid and vol.uuid = :volUuid";
        return CEPH_PRIMARY_STORAGE_TYPE.equals(SQL.New(sql, String.class).param("volUuid", volumeUuid).find());
    }

    @Override
    public void volumeSnapshotAfterCleanUpExtensionPoint(String volumeUuid, List<VolumeSnapshotInventory> snapshots) {
        if (CollectionUtils.isEmpty(snapshots) || !isCephPs(volumeUuid)) {
            return;
        }

        Set<String> volumeInstallPaths = snapshots.stream().map(sp -> getVolumeInstallPathFromSnapshot(sp.getPrimaryStorageInstallPath()))
                .collect(Collectors.toSet());
        if (volumeInstallPaths.isEmpty()) {
            return;
        }

        volumeInstallPaths.forEach(volumeInstallPath -> {
            String details = trash.makeSureInstallPathNotUsed(volumeInstallPath, VolumeVO.class.getSimpleName());

            if (StringUtils.isBlank(details)) {
                logger.debug(String.format("delete volume[InstallPath:%s] after cleaning up snapshots", volumeInstallPath));
                DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(snapshots.get(0).getPrimaryStorageUuid());
                msg.setInstallPath(volumeInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, snapshots.get(0).getPrimaryStorageUuid());
                bus.send(msg);
            }
        });
    }
}
