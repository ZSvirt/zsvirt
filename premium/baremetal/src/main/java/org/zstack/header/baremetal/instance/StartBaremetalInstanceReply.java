package org.zstack.header.baremetal.instance;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 7/6/18.
 */
public class StartBaremetalInstanceReply extends MessageReply {
    private BaremetalInstanceInventory inventory;

    public BaremetalInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalInstanceInventory inventory) {
        this.inventory = inventory;
    }
}
