package org.zstack.header.volume;

public class CreateDataVolumeTemplateFromDataVolumeSnapshotMsg extends CreateDataVolumeTemplateFromDataVolumeMsg {
    private String snapshotUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
