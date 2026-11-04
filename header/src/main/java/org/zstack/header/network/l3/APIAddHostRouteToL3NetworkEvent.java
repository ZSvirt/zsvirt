package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddHostRouteToL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L3NetworkInventory`
     */
    private L3NetworkInventory inventory;

    public APIAddHostRouteToL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public APIAddHostRouteToL3NetworkEvent() {
        super(null);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddHostRouteToL3NetworkEvent __example__() {
        APIAddHostRouteToL3NetworkEvent event = new APIAddHostRouteToL3NetworkEvent();
        event.setInventory(L3NetworkInventory.__example__());
        return event;
    }
}
