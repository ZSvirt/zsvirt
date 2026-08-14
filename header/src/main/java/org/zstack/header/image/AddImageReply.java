package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * Created by camile on 2/5/2018.
 */
public class AddImageReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }

    public boolean needTrack() {
        return inventory != null && ImageStatus.Downloading.toString().equals(inventory.getStatus());
    }
}
