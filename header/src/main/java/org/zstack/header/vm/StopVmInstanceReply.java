package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class StopVmInstanceReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
