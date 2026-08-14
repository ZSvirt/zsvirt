package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Create by weiwang at 2018/8/30
 */
public class SyncPrimaryStorageCapacityReply extends MessageReply {
    private PrimaryStorageInventory inventory;

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
