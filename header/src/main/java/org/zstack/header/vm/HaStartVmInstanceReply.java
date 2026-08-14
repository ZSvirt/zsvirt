package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaStartVmInstanceReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
