package org.zstack.sdk;

import org.zstack.sdk.WebhookInventory;

public class CreateWebhookResult {
    public WebhookInventory inventory;
    public void setInventory(WebhookInventory inventory) {
        this.inventory = inventory;
    }
    public WebhookInventory getInventory() {
        return this.inventory;
    }

}
