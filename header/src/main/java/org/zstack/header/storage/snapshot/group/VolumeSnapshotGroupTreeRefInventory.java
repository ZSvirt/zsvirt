package org.zstack.header.storage.snapshot.group;

import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

public class VolumeSnapshotGroupTreeRefInventory {
    private String volumeUuid;
    private String volumeName;
    private String volumeType;
    private String volumeSnapshotUuid;
    private boolean snapshotDeleted;
    private VolumeSnapshotInventory snapshot;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public boolean isSnapshotDeleted() {
        return snapshotDeleted;
    }

    public void setSnapshotDeleted(boolean snapshotDeleted) {
        this.snapshotDeleted = snapshotDeleted;
    }

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }
}
