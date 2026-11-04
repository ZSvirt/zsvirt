package org.zstack.accessKey;

import org.zstack.header.message.MessageReply;

public class CreateAccessKeyReply extends MessageReply {
    private AccessKeyInventory inventory;

    public AccessKeyInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccessKeyInventory inventory) {
        this.inventory = inventory;
    }
}
