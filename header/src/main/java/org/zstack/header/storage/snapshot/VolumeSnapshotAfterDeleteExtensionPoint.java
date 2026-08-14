package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeSnapshotAfterDeleteExtensionPoint {
    void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion);
    void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot);
    void volumeSnapshotAfterCleanUpExtensionPoint(String volumeUuid, List<VolumeSnapshotInventory> snapshots);
}
