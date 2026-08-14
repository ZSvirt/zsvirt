package org.zstack.sdk;

import org.zstack.sdk.CephPrimaryStorageInventory;

public class UpdateCephPrimaryStorageMonResult {
    public CephPrimaryStorageInventory inventory;
    public void setInventory(CephPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public CephPrimaryStorageInventory getInventory() {
        return this.inventory;
    }

}
