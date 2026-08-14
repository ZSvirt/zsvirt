package org.zstack.zwatch.resnotify;

import org.zstack.header.query.APIQueryReply;
import java.util.List;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventories")
public class APIQueryResNotifySubscriptionReply extends APIQueryReply {
    private List<ResNotifySubscriptionInventory> inventories;

    public List<ResNotifySubscriptionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResNotifySubscriptionInventory> inventories) {
        this.inventories = inventories;
    }
}
