package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRenewSessionEvent extends APIEvent {
    private SessionInventory inventory;

    public APIRenewSessionEvent(String apiId) {
        super(apiId);
    }

    public APIRenewSessionEvent() {
        super(null);
    }

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRenewSessionEvent __example__() {
        APIRenewSessionEvent event = new APIRenewSessionEvent();
        event.setInventory(SessionInventory.__example__());
        return event;
    }
}