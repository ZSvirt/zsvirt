package org.zstack.vrouterRoute;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventory")
public class APIAddVRouterRouteEntryEvent extends APIEvent {
    private VRouterRouteEntryInventory inventory;

    public APIAddVRouterRouteEntryEvent(String apiId) {
        super(apiId);
    }

    public APIAddVRouterRouteEntryEvent() {
        super();
    }

    public VRouterRouteEntryInventory getInventory() {
        return inventory;
    }

    public void setInventory(VRouterRouteEntryInventory inv) {
        this.inventory = inv;
    }

    public static APIAddVRouterRouteEntryEvent __example__() {
        APIAddVRouterRouteEntryEvent evt = new APIAddVRouterRouteEntryEvent();
        VRouterRouteEntryInventory inv = new VRouterRouteEntryInventory();
        inv.setDescription("Test route");
        inv.setType(VRouterRouteEntryType.UserStatic);
        inv.setRouteTableUuid(uuid());
        inv.setDestination("192.168.2.0/24");
        inv.setTarget("172.20.1.1");
        inv.setDistance(128);

        evt.setInventory(inv);
        evt.setSuccess(true);
        return evt;
    }
}
