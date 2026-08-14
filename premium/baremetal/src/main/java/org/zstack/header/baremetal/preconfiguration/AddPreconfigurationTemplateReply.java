package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2018-12-26.
 */
public class AddPreconfigurationTemplateReply extends MessageReply {
    private PreconfigurationTemplateInventory inventory;

    public PreconfigurationTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(PreconfigurationTemplateInventory inventory) {
        this.inventory = inventory;
    }
}
