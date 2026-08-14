package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2019-01-23.
 */
public class DetachBaremetalPxeServerFromClusterReply extends MessageReply {
    private BaremetalPxeServerInventory inventory;

    public BaremetalPxeServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(BaremetalPxeServerInventory inventory) {
        this.inventory = inventory;
    }
}
