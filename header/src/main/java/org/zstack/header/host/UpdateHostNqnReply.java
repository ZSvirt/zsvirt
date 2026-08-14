package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class UpdateHostNqnReply extends MessageReply {
    HostInventory inventory;

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
}
