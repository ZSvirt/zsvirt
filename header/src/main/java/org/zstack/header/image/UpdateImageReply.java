package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2020/7/8.
 */
public class UpdateImageReply extends MessageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
