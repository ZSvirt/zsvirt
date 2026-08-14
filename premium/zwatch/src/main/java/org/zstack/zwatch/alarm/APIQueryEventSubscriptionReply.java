package org.zstack.zwatch.alarm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryEventSubscriptionReply extends APIQueryReply {
    private List<EventSubscriptionInventory> inventories;

    public static APIQueryEventSubscriptionReply __example__() {
        APIQueryEventSubscriptionReply ret = new APIQueryEventSubscriptionReply();
        ret.inventories = asList(EventSubscriptionInventory.__example__());
        return ret;
    }

    public List<EventSubscriptionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EventSubscriptionInventory> inventories) {
        this.inventories = inventories;
    }
}
