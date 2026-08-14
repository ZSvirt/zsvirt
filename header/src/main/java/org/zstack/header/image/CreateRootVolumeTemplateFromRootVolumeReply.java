package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

/**
 * Created by camile on 2/6/2018.
 */
public class CreateRootVolumeTemplateFromRootVolumeReply extends MessageReply implements ImageReply {
    private ImageInventory inventory;

    @Override
    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
