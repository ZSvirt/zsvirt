package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class PrimaryStorageRemoveCachedImageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private ImageCacheInventory inventory;

    public ImageCacheInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageCacheInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return inventory.getPrimaryStorageUuid();
    }
}
