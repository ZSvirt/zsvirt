package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class FlattenVolumeReply extends MessageReply {
    private VolumeInventory inventory;

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }
}
