package org.zstack.storage.snapshot;

import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * Created by liangbo.zhou on 17-6-27.
 */
public interface PostMarkRootVolumeAsSnapshotExtension {
     void afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory snapshot);
}
