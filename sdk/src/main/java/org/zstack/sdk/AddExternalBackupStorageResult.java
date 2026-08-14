package org.zstack.sdk;

import org.zstack.sdk.ExternalBackupStorageInventory;

public class AddExternalBackupStorageResult {
    public ExternalBackupStorageInventory inventory;
    public void setInventory(ExternalBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalBackupStorageInventory getInventory() {
        return this.inventory;
    }

}
