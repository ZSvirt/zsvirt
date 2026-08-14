package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 */
public class AllocateBackupStorageReply extends MessageReply {
    private BackupStorageInventory inventory;

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
