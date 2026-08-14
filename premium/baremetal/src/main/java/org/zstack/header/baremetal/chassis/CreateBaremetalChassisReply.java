package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2018-10-08.
 */
public class CreateBaremetalChassisReply extends MessageReply {
    private BaremetalChassisInventory inventory;

    public BaremetalChassisInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalChassisInventory inventory) {
        this.inventory = inventory;
    }
}
