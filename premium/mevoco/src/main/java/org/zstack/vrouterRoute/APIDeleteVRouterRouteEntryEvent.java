package org.zstack.vrouterRoute;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventory")
public class APIDeleteVRouterRouteEntryEvent extends APIEvent {
    private VRouterRouteTableInventory inventory;

    public APIDeleteVRouterRouteEntryEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteVRouterRouteEntryEvent() {
        super(null);
    }

    public VRouterRouteTableInventory getInventory() {
        return inventory;
    }

    public void setInventory(VRouterRouteTableInventory inventory) {
        this.inventory = inventory;
    }

    public static APIDeleteVRouterRouteEntryEvent __example__() {
        APIDeleteVRouterRouteEntryEvent evt = new APIDeleteVRouterRouteEntryEvent();

        evt.setSuccess(true);
        return evt;
    }
}
