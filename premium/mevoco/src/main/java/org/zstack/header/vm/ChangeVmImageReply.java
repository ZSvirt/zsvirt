package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 11/2/17.
 */
public class ChangeVmImageReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
