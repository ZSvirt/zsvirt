package org.zstack.sdk;

import org.zstack.sdk.CephBackupStorageInventory;

public class AddMonToCephBackupStorageResult {
    public CephBackupStorageInventory inventory;
    public void setInventory(CephBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public CephBackupStorageInventory getInventory() {
        return this.inventory;
    }

}
