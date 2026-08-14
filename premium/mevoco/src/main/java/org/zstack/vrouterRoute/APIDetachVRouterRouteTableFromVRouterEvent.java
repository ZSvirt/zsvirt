package org.zstack.vrouterRoute;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.HashSet;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventory")
public class APIDetachVRouterRouteTableFromVRouterEvent extends APIEvent {
    private VRouterRouteTableInventory inventory;

    public APIDetachVRouterRouteTableFromVRouterEvent(String apiId) {
        super(apiId);
    }

    public APIDetachVRouterRouteTableFromVRouterEvent() {
        super(null);
    }

    public VRouterRouteTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(VRouterRouteTableInventory inv) {
        this.inventory = inv;
    }

    public static APIDetachVRouterRouteTableFromVRouterEvent __example__() {
        APIDetachVRouterRouteTableFromVRouterEvent evt = new APIDetachVRouterRouteTableFromVRouterEvent();
        VRouterRouteTableInventory inv = new VRouterRouteTableInventory();
        inv.setDescription("Test route table");
        inv.setName("test-route-table");

        inv.setAttachedRouterRefs(asList());
        inv.setRouteEntries(asList());

        evt.setInventory(inv);
        evt.setSuccess(true);
        return evt;
    }
}
