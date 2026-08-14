package org.zstack.header.storage.snapshot;

import org.zstack.header.message.CancelMessage;

public class CancelDeleteVolumeSnapshotMsg extends CancelMessage implements VolumeSnapshotMessage {
    private String snapshotUuid;
    private String volumeUuid;
    private String treeUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
