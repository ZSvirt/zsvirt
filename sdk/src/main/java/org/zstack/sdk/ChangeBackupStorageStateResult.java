package org.zstack.sdk;

import org.zstack.sdk.BackupStorageInventory;

public class ChangeBackupStorageStateResult {
    public BackupStorageInventory inventory;
    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public BackupStorageInventory getInventory() {
        return this.inventory;
    }

}
