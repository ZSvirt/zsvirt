package org.zstack.sdk;

import org.zstack.sdk.CephBackupStorageInventory;

public class RemoveMonFromCephBackupStorageResult {
    public CephBackupStorageInventory inventory;
    public void setInventory(CephBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public CephBackupStorageInventory getInventory() {
        return this.inventory;
    }

}
