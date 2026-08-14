package org.zstack.header.storage.primary;

public class CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg extends CreateTemplateFromVolumeOnPrimaryStorageMsg {
    private String snapshotUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
