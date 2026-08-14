package org.zstack.header.storage.snapshot;

import org.zstack.header.core.Completion;

public interface VolumeSnapshotDeletionProtector {
    String getPrimaryStorageType();

    void protect(VolumeSnapshotInventory snapshot, Completion completion);
}
