package org.zstack.storage.addon.primary;

import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeDeletionPolicyManager;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.volume.ChangeVolumeProcessingMethodExtensionPoint;

/**
 * @author Xingwei Yu
 * @date 2024/10/23 16:07
 */
public class ExternalPrimaryStorageChangeVolumeProcessingMethodExtension implements ChangeVolumeProcessingMethodExtensionPoint {
    @Override
    public VolumeDeletionPolicyManager.VolumeDeletionPolicy getTransientVolumeDeletionPolicy(VolumeInventory transientVolume) {
        String psType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, transientVolume.getPrimaryStorageUuid()).select(PrimaryStorageVO_.type).findValue();
        if (!psType.equals(PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE)) {
            return null;
        }
        boolean hasSnapshots = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageUuid, transientVolume.getPrimaryStorageUuid())
                .like(VolumeSnapshotVO_.primaryStorageInstallPath, String.format("%s@%%", transientVolume.getInstallPath())).isExists();
        if (!hasSnapshots) {
            return VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct;
        }
        return VolumeDeletionPolicyManager.VolumeDeletionPolicy.DBOnly;
    }
}
