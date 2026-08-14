package org.zstack.vrouterRoute;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVRouterRouteEntryReply extends APIQueryReply {
    private List<VRouterRouteEntryInventory> inventories = new ArrayList<>();

    public List<VRouterRouteEntryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VRouterRouteEntryInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVRouterRouteEntryReply __example__() {
        APIQueryVRouterRouteEntryReply reply = new APIQueryVRouterRouteEntryReply();
        VRouterRouteEntryInventory inv = new VRouterRouteEntryInventory();
        inv.setDescription("Test route");
        inv.setType(VRouterRouteEntryType.UserStatic);
        inv.setRouteTableUuid(uuid());
        inv.setDestination("192.168.2.0/24");
        inv.setTarget("172.20.1.1");

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
