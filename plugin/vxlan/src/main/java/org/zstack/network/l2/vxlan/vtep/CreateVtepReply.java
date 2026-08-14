package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.message.MessageReply;

/**
 * Created by weiwang on 21/04/2017.
 */
public class CreateVtepReply extends MessageReply {
    private VtepInventory inventory;

    public VtepInventory getInventory() {
        return inventory;
    }

    public void setInventory(VtepInventory inventory) {
        this.inventory = inventory;
    }
}
