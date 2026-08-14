package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by camile on 3/7/2018.
 */
public class MigrateVmInnerReply extends MessageReply {
    private VmInstanceInventory inventory;

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
