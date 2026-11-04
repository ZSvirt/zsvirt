package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/16/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateIpRangeEvent extends APIEvent {
    private IpRangeInventory inventory;

    public APIUpdateIpRangeEvent() {
    }

    public APIUpdateIpRangeEvent(String apiId) {
        super(apiId);
    }

    public IpRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(IpRangeInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateIpRangeEvent __example__() {
        APIUpdateIpRangeEvent event = new APIUpdateIpRangeEvent();
        event.setInventory(IpRangeInventory.__example__());
        return event;
    }
}
