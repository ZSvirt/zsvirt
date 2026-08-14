package org.zstack.storage.snapshot;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * @Author: DaoDao
 * @Date: 2021/11/12
 */
public interface BeforeRevertVolumeFromSnapshotExtensionPoint {
    void beforeRevertVolumeFromSnapshot(VolumeSnapshotInventory inventory, Completion completion);
}
