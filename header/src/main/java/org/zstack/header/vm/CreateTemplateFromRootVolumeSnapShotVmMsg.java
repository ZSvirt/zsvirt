package org.zstack.header.vm;

public class CreateTemplateFromRootVolumeSnapShotVmMsg extends CreateTemplateFromRootVolumeVmMsg {
    private String snapshotUuid;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}
