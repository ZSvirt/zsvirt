package org.zstack.header.cbt;

import org.zstack.header.message.MessageReply;

public class CreateCbtTaskReply extends MessageReply {
    private CbtTaskInventory inventory;

    public CbtTaskInventory getInventory() {
        return inventory;
    }

    public void setInventory(CbtTaskInventory inventory) {
        this.inventory = inventory;
    }
}
