package org.zstack.header.core.webhooks;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/5/7.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateWebhookEvent extends APIEvent {
    private WebhookInventory inventory;

    public APIUpdateWebhookEvent() {
    }

    public APIUpdateWebhookEvent(String apiId) {
        super(apiId);
    }

    public WebhookInventory getInventory() {
        return inventory;
    }

    public void setInventory(WebhookInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateWebhookEvent __example__() {
        return new APIUpdateWebhookEvent();
    }
}
