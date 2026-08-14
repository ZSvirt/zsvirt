package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class InspectBaremetalChassisReply extends MessageReply {
    private BaremetalChassisInventory inventory;

    public BaremetalChassisInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalChassisInventory inventory) {
        this.inventory = inventory;
    }
}
