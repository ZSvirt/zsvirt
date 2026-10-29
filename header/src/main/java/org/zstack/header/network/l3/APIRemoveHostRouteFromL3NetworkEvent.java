package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRemoveHostRouteFromL3NetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L3NetworkInventory`
     */
    private L3NetworkInventory inventory;

    public APIRemoveHostRouteFromL3NetworkEvent() {
        super(null);
    }

    public APIRemoveHostRouteFromL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveHostRouteFromL3NetworkEvent __example__() {
        APIRemoveHostRouteFromL3NetworkEvent event = new APIRemoveHostRouteFromL3NetworkEvent();
        event.setInventory(L3NetworkInventory.__example__());
        return event;
    }
}
