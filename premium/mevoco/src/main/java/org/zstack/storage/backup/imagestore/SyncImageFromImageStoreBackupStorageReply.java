package org.zstack.storage.backup.imagestore;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;

public class SyncImageFromImageStoreBackupStorageReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
