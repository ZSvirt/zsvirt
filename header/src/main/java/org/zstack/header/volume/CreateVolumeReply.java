package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class CreateVolumeReply extends MessageReply {
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
}
