package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventory")
public class APIChangeInstanceOfferingStateEvent extends APIEvent {
    private InstanceOfferingInventory inventory;

    public APIChangeInstanceOfferingStateEvent() {
        super(null);
    }

    public APIChangeInstanceOfferingStateEvent(String apiId) {
        super(apiId);
    }

    public InstanceOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(InstanceOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeInstanceOfferingStateEvent __example__() {
        APIChangeInstanceOfferingStateEvent event = new APIChangeInstanceOfferingStateEvent();
        event.setInventory(InstanceOfferingInventory.__example__());
        return event;
    }
}
