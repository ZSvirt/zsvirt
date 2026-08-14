package org.zstack.sdk;

import org.zstack.sdk.VolumeInventory;

public class FlattenVolumeResult {
    public VolumeInventory inventory;
    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeInventory getInventory() {
        return this.inventory;
    }

}
