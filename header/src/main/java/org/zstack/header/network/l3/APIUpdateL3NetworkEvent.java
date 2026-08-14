package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateL3NetworkEvent extends APIEvent {
    private L3NetworkInventory inventory;

    public APIUpdateL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateL3NetworkEvent() {
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateL3NetworkEvent __example__() {
        APIUpdateL3NetworkEvent event = new APIUpdateL3NetworkEvent();
        L3NetworkInventory l3 = new L3NetworkInventory();

        l3.setName("Test-L3Network");
        l3.setUuid(uuid());

        event.setInventory(l3);
        return event;
    }

}
