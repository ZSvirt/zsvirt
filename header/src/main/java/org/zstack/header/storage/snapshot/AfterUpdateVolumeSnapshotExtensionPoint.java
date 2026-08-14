package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

public interface AfterUpdateVolumeSnapshotExtensionPoint {
    void afterUpdateVolumeSnapshot(VolumeSnapshotVO vo, APIUpdateVolumeSnapshotMsg msg, Completion completion);
}
