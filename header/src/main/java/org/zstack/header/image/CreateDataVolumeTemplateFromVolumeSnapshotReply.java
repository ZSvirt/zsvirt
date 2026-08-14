package org.zstack.header.image;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by MaJin on 2021/3/16.
 */
public class CreateDataVolumeTemplateFromVolumeSnapshotReply extends MessageReply implements ImageReply {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
