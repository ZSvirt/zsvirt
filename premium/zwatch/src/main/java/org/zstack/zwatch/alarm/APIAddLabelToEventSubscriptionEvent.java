package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddLabelToEventSubscriptionEvent extends APIEvent {
    private EventSubscriptionLabelInventory inventory;

    public APIAddLabelToEventSubscriptionEvent() {
    }

    public APIAddLabelToEventSubscriptionEvent(String apiId) {
        super(apiId);
    }

    public EventSubscriptionLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventSubscriptionLabelInventory inventory) {
        this.inventory = inventory;
    }
}
