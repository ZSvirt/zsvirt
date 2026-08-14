package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

import static java.util.Arrays.asList;

/**
 */
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
        L3NetworkInventory l3 = new L3NetworkInventory();
        L3NetworkHostRouteInventory route = new L3NetworkHostRouteInventory();

        route.setPrefix("169.254.169.254/32");
        route.setPrefix("192.168.1.254");

        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());
        l3.setDns(Arrays.asList("8.8.8.8"));
        l3.setHostRoute(asList(route));

        event.setInventory(l3);
        return event;
    }

}
