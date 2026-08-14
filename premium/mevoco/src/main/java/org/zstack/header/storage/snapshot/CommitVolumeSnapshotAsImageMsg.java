package org.zstack.header.storage.snapshot;

import org.zstack.header.storage.primary.CommitVolumeAsImageMsg;

public class CommitVolumeSnapshotAsImageMsg extends CommitVolumeAsImageMsg {
    private String volumeSnapshotUuid;

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }
}
