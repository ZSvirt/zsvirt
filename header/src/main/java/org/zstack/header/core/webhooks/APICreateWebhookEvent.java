package org.zstack.header.core.webhooks;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/5/7.
 */
@RestResponse(allTo = "inventory")
public class APICreateWebhookEvent extends APIEvent {
    private WebhookInventory inventory;

    public APICreateWebhookEvent() {
    }

    public APICreateWebhookEvent(String apiId) {
        super(apiId);
    }

    public WebhookInventory getInventory() {
        return inventory;
    }

    public void setInventory(WebhookInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateWebhookEvent __example__() {
        return new APICreateWebhookEvent();
    }
}
