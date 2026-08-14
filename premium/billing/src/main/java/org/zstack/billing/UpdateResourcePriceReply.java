package org.zstack.billing;

import org.zstack.header.message.MessageReply;

/**
 * Created by lining on 2019/4/25.
 */
public class UpdateResourcePriceReply extends MessageReply {
    private PriceInventory inventory;

    public PriceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PriceInventory inventory) {
        this.inventory = inventory;
    }
}
