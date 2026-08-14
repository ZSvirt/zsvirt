package org.zstack.vrouterRoute;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventory")
public class APICreateVRouterRouteTableEvent extends APIEvent {
    private VRouterRouteTableInventory inventory;

    public APICreateVRouterRouteTableEvent(String apiId) {
        super(apiId);
    }

    public APICreateVRouterRouteTableEvent() {
        super(null);
    }

    public VRouterRouteTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(VRouterRouteTableInventory inv) {
        this.inventory = inv;
    }

    public static APICreateVRouterRouteTableEvent __example__() {
        APICreateVRouterRouteTableEvent evt = new APICreateVRouterRouteTableEvent();
        VRouterRouteTableInventory inv = new VRouterRouteTableInventory();
        inv.setDescription("Test route table");
        inv.setName("test-route-table");
        inv.setAttachedRouterRefs(new ArrayList<>());
        inv.setRouteEntries(new ArrayList<>());

        evt.setInventory(inv);
        evt.setSuccess(true);
        return evt;
    }
}
