package org.zstack.header.cloudformation;

import org.zstack.header.message.MessageReply;

/**
 * Created by mingjian.deng on 2019/6/4.
 */
public class CreateStackResourceReply extends MessageReply {
    private ResourceStackInventory inventory;

    public ResourceStackInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceStackInventory inventory) {
        this.inventory = inventory;
    }
}
