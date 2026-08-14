package org.zstack.sdk;

import org.zstack.sdk.CephPrimaryStoragePoolInventory;

public class UpdateCephPrimaryStoragePoolResult {
    public CephPrimaryStoragePoolInventory inventory;
    public void setInventory(CephPrimaryStoragePoolInventory inventory) {
        this.inventory = inventory;
    }
    public CephPrimaryStoragePoolInventory getInventory() {
        return this.inventory;
    }

}
