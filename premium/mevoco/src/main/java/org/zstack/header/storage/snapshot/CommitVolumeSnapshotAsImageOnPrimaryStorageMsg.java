package org.zstack.header.storage.snapshot;

import org.zstack.header.storage.primary.CommitVolumeAsImageOnPrimaryStorageMsg;

public class CommitVolumeSnapshotAsImageOnPrimaryStorageMsg extends CommitVolumeAsImageOnPrimaryStorageMsg {
    private String snapshotUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
