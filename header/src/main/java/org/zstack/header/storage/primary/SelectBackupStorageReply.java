package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;

/**
 * Created by mingjian.deng on 2019/10/10.
 */
public class SelectBackupStorageReply extends MessageReply {
    private BackupStorageInventory inventory;

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
