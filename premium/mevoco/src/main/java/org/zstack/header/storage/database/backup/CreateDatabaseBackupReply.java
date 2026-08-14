package org.zstack.header.storage.database.backup;

import org.zstack.header.message.MessageReply;


public class CreateDatabaseBackupReply extends MessageReply {
    private DatabaseBackupInventory inventory;

    public void setInventory(DatabaseBackupInventory inventory) {
        this.inventory = inventory;
    }

    public DatabaseBackupInventory getInventory() {
        return inventory;
    }
}
