package org.zstack.zwatch.alarm;

import org.zstack.header.message.MessageReply;

public class SubscribeEventReply extends MessageReply {
    private EventSubscriptionInventory inventory;

    public EventSubscriptionInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventSubscriptionInventory inventory) {
        this.inventory = inventory;
    }
}
