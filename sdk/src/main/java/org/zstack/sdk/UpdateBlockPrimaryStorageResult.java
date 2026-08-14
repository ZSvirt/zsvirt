package org.zstack.sdk;

import org.zstack.sdk.BlockPrimaryStorageInventory;

public class UpdateBlockPrimaryStorageResult {
    public BlockPrimaryStorageInventory inventory;
    public void setInventory(BlockPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public BlockPrimaryStorageInventory getInventory() {
        return this.inventory;
    }

}
