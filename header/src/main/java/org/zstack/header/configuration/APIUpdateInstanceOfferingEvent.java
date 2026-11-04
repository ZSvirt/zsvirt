package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateInstanceOfferingEvent extends APIEvent {
    private InstanceOfferingInventory inventory;

    public APIUpdateInstanceOfferingEvent() {
    }

    public APIUpdateInstanceOfferingEvent(String apiId) {
        super(apiId);
    }

    public InstanceOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(InstanceOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateInstanceOfferingEvent __example__() {
        APIUpdateInstanceOfferingEvent event = new APIUpdateInstanceOfferingEvent();
        event.setInventory(InstanceOfferingInventory.__example__());
        return event;
    }

}
