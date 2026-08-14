package org.zstack.storage.migration.backup;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 10/14/17.
 */
public class MigrateImageOnBackupStorageReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
