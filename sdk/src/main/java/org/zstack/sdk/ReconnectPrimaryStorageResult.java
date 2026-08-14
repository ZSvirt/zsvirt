package org.zstack.sdk;

import org.zstack.sdk.PrimaryStorageInventory;

public class ReconnectPrimaryStorageResult {
    public PrimaryStorageInventory inventory;
    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public PrimaryStorageInventory getInventory() {
        return this.inventory;
    }

}
