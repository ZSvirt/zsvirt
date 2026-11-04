package org.zstack.header.network.l2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateL2NetworkEvent extends APIEvent {
    private L2NetworkInventory inventory;

    public L2NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateL2NetworkEvent() {
    }

    public APIUpdateL2NetworkEvent(String apiId) {
        super(apiId);
    }
 
    public static APIUpdateL2NetworkEvent __example__() {
        APIUpdateL2NetworkEvent event = new APIUpdateL2NetworkEvent();
        event.setInventory(L2VlanNetworkInventory.__example__());
        return event;
    }
}
