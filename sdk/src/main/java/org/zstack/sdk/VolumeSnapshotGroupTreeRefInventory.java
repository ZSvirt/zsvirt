package org.zstack.sdk;

import org.zstack.sdk.VolumeSnapshotInventory;

public class VolumeSnapshotGroupTreeRefInventory  {

    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public java.lang.String volumeName;
    public void setVolumeName(java.lang.String volumeName) {
        this.volumeName = volumeName;
    }
    public java.lang.String getVolumeName() {
        return this.volumeName;
    }

    public java.lang.String volumeType;
    public void setVolumeType(java.lang.String volumeType) {
        this.volumeType = volumeType;
    }
    public java.lang.String getVolumeType() {
        return this.volumeType;
    }

    public java.lang.String volumeSnapshotUuid;
    public void setVolumeSnapshotUuid(java.lang.String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }
    public java.lang.String getVolumeSnapshotUuid() {
        return this.volumeSnapshotUuid;
    }

    public boolean snapshotDeleted;
    public void setSnapshotDeleted(boolean snapshotDeleted) {
        this.snapshotDeleted = snapshotDeleted;
    }
    public boolean getSnapshotDeleted() {
        return this.snapshotDeleted;
    }

    public VolumeSnapshotInventory snapshot;
    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }
    public VolumeSnapshotInventory getSnapshot() {
        return this.snapshot;
    }

}
