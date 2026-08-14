package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddActionToEventSubscriptionEvent extends APIEvent {
    private EventSubscriptionInventory inventory;

    public APIAddActionToEventSubscriptionEvent() {
    }

    public APIAddActionToEventSubscriptionEvent(String apiId) {
        super(apiId);
    }

    public EventSubscriptionInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventSubscriptionInventory inventory) {
        this.inventory = inventory;
    }
}
