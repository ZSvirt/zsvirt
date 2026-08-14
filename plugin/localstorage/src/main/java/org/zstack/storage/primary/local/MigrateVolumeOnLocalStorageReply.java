package org.zstack.storage.primary.local;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/7/21.
 */
public class MigrateVolumeOnLocalStorageReply extends MessageReply {
    private LocalStorageResourceRefInventory inventory;

    public LocalStorageResourceRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(LocalStorageResourceRefInventory inventory) {
        this.inventory = inventory;
    }
}
