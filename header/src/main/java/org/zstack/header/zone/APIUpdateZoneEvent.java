package org.zstack.header.zone;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateZoneEvent extends APIEvent {
    private ZoneInventory inventory;

    public APIUpdateZoneEvent() {
    }

    public APIUpdateZoneEvent(String apiId) {
        super(apiId);
    }

    public ZoneInventory getInventory() {
        return inventory;
    }

    public void setInventory(ZoneInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateZoneEvent __example__() {
        APIUpdateZoneEvent event = new APIUpdateZoneEvent();
        event.setInventory(ZoneInventory.__example__());
        return event;
    }

}
