package org.zstack.sdk.zwatch.resnotify;

import org.zstack.sdk.zwatch.resnotify.ResNotifySubscriptionInventory;

public class UpdateResNotifySubscriptionResult {
    public ResNotifySubscriptionInventory inventory;
    public void setInventory(ResNotifySubscriptionInventory inventory) {
        this.inventory = inventory;
    }
    public ResNotifySubscriptionInventory getInventory() {
        return this.inventory;
    }

}
