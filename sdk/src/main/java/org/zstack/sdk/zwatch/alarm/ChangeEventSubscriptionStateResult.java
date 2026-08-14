package org.zstack.sdk.zwatch.alarm;

import org.zstack.sdk.zwatch.alarm.EventSubscriptionInventory;

public class ChangeEventSubscriptionStateResult {
    public EventSubscriptionInventory inventory;
    public void setInventory(EventSubscriptionInventory inventory) {
        this.inventory = inventory;
    }
    public EventSubscriptionInventory getInventory() {
        return this.inventory;
    }

}
