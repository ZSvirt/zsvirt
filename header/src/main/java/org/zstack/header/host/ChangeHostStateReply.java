package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class ChangeHostStateReply extends MessageReply {
    private HostInventory inventory;

    public ChangeHostStateReply() {
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

}
