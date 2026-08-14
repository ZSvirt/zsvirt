package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 */
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
        L3NetworkInventory l3 = new L3NetworkInventory();

        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());
        l3.setDns(Arrays.asList());

        event.setInventory(l3);
        return event;
    }

}
