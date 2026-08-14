package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2023/5/16 13:17
 */
@RestResponse(allTo = "inventory")
public class APIChangeEventSubscriptionStateEvent extends APIEvent {
    private EventSubscriptionInventory inventory;

    public APIChangeEventSubscriptionStateEvent () {
    }

    public APIChangeEventSubscriptionStateEvent (String apiId) {
        super(apiId);
    }

    public EventSubscriptionInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventSubscriptionInventory inventory) {
        this.inventory = inventory;
    }
}
