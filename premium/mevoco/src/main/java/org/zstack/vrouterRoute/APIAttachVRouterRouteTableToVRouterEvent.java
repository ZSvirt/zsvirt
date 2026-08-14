package org.zstack.vrouterRoute;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventory")
public class APIAttachVRouterRouteTableToVRouterEvent extends APIEvent {
    private VRouterRouteTableInventory inventory;

    public APIAttachVRouterRouteTableToVRouterEvent(String apiId) {
        super(apiId);
    }

    public APIAttachVRouterRouteTableToVRouterEvent() {
        super(null);
    }

    public VRouterRouteTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(VRouterRouteTableInventory inv) {
        this.inventory = inv;
    }

    public static APIAttachVRouterRouteTableToVRouterEvent __example__() {
        APIAttachVRouterRouteTableToVRouterEvent evt = new APIAttachVRouterRouteTableToVRouterEvent();
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
