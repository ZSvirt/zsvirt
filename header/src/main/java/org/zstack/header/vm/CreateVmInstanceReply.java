package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by david on 8/4/16.
 */
public class CreateVmInstanceReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
