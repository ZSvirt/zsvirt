package org.zstack.sdk.databasebackup;

import org.zstack.sdk.databasebackup.DatabaseBackupInventory;

public class CreateDatabaseBackupResult {
    public DatabaseBackupInventory inventory;
    public void setInventory(DatabaseBackupInventory inventory) {
        this.inventory = inventory;
    }
    public DatabaseBackupInventory getInventory() {
        return this.inventory;
    }

}
