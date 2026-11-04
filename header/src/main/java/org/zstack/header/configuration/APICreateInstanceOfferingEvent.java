package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateInstanceOfferingEvent extends APIEvent {
    private InstanceOfferingInventory inventory;

    public InstanceOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(InstanceOfferingInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateInstanceOfferingEvent() {
        super(null);
    }

    public APICreateInstanceOfferingEvent(String apiId) {
        super(apiId);
    }

 
    public static APICreateInstanceOfferingEvent __example__() {
        APICreateInstanceOfferingEvent event = new APICreateInstanceOfferingEvent();
        event.setInventory(InstanceOfferingInventory.__example__());
        return event;
    }
}
