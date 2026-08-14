package org.zstack.header.storage.snapshot;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeSnapshotBeforeDeleteExtensionPoint {
    void volumeSnapshotBeforeDeleteExtensionPoint(VolumeSnapshotInventory snapshot);
}
