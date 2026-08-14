package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

public abstract class CreateImageReply extends MessageReply implements ImageReply {
    private ImageInventory inventory;

    @Override
    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
