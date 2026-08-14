package org.zstack.header.storage.snapshot;

import java.util.List;

public class VolumeSnapshotDeletionStructs {
    List<VolumeSnapshotInventory> snapshotInventories;
    String direction;
    String scope;

    public VolumeSnapshotDeletionStructs(List<VolumeSnapshotInventory> snapshotInventories, String direction, String scope) {
        this.snapshotInventories = snapshotInventories;
        this.direction = direction;
        this.scope = scope;
    }

    public List<VolumeSnapshotInventory> getSnapshotInventories() {
        return snapshotInventories;
    }

    public void setSnapshotInventories(List<VolumeSnapshotInventory> snapshotInventories) {
        this.snapshotInventories = snapshotInventories;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
