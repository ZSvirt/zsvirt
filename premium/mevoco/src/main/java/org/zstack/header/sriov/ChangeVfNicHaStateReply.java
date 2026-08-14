package org.zstack.header.sriov;

import org.zstack.header.message.MessageReply;

public class ChangeVfNicHaStateReply extends MessageReply {
    private VmVfNicInventory inventory;

    public VmVfNicInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmVfNicInventory inventory) {
        this.inventory = inventory;
    }
}
