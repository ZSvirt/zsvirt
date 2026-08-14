package org.zstack.sdk;

import org.zstack.sdk.ExternalBackupInventory;

public class CreateExternalBackupResult {
    public ExternalBackupInventory inventory;
    public void setInventory(ExternalBackupInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalBackupInventory getInventory() {
        return this.inventory;
    }

}
