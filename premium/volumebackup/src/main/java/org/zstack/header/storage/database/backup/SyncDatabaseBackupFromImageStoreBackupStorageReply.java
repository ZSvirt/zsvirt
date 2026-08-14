package org.zstack.header.storage.database.backup;

import org.zstack.header.message.MessageReply;

public class SyncDatabaseBackupFromImageStoreBackupStorageReply extends MessageReply {
    private DatabaseBackupInventory inventory;

    public DatabaseBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(DatabaseBackupInventory inventory) {
        this.inventory = inventory;
    }
}
