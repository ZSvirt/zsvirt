package org.zstack.sdk;

import org.zstack.sdk.BlockVolumeInventory;

public class UpdateBlockVolumeResult {
    public BlockVolumeInventory inventory;
    public void setInventory(BlockVolumeInventory inventory) {
        this.inventory = inventory;
    }
    public BlockVolumeInventory getInventory() {
        return this.inventory;
    }

}
