package org.zstack.sdk;

import org.zstack.sdk.VolumeSnapshotInventory;

public class UpdateVolumeSnapshotResult {
    public VolumeSnapshotInventory inventory;
    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeSnapshotInventory getInventory() {
        return this.inventory;
    }

}
