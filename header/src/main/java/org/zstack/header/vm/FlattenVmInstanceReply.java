package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class FlattenVmInstanceReply extends MessageReply {
    private VmInstanceInventory inventory;

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }
}
