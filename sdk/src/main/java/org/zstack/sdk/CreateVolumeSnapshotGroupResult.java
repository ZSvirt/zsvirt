package org.zstack.sdk;

import org.zstack.sdk.VolumeSnapshotGroupInventory;

public class CreateVolumeSnapshotGroupResult {
    public VolumeSnapshotGroupInventory inventory;
    public void setInventory(VolumeSnapshotGroupInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeSnapshotGroupInventory getInventory() {
        return this.inventory;
    }

}
