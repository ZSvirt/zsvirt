package org.zstack.vrouterRoute;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
;

/**
 * Created by fang.sun on 1/24/2018.
 */

@RestResponse(allTo = "inventory")
public class APIUpdateVRouterRouteTableEvent extends APIEvent {
    private VRouterRouteTableInventory inventory;

    public APIUpdateVRouterRouteTableEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateVRouterRouteTableEvent() {
        super(null);
    }

    public VRouterRouteTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(VRouterRouteTableInventory inv) {
        this.inventory = inv;
    }

    public static APIUpdateVRouterRouteTableEvent __example__() {
        APIUpdateVRouterRouteTableEvent evt = new APIUpdateVRouterRouteTableEvent();
        VRouterRouteTableInventory inv = new VRouterRouteTableInventory();
        inv.setDescription("Test route table");
        inv.setName("test-route-table");
        inv.setUuid(uuid());
        evt.setInventory(inv);
        evt.setSuccess(true);
        return evt;
    }
}


