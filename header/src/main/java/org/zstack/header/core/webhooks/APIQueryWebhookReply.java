package org.zstack.header.core.webhooks;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by xing5 on 2017/5/7.
 */
@RestResponse(allTo = "inventories")
public class APIQueryWebhookReply extends APIQueryReply {
    private List<WebhookInventory> inventories;

    public List<WebhookInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<WebhookInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryWebhookReply __example__() {
        return new APIQueryWebhookReply();
    }
}
