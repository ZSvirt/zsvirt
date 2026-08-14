package org.zstack.vrouterRoute;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 17/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVirtualRouterVRouterRouteTableRefReply extends APIQueryReply {
    private List<VirtualRouterVRouterRouteTableRefInventory> inventories;

    public List<VirtualRouterVRouterRouteTableRefInventory>  getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterVRouterRouteTableRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVirtualRouterVRouterRouteTableRefReply __example__() {
        APIQueryVirtualRouterVRouterRouteTableRefReply reply = new APIQueryVirtualRouterVRouterRouteTableRefReply();
        VirtualRouterVRouterRouteTableRefInventory inv = new VirtualRouterVRouterRouteTableRefInventory();
        inv.setRouteTableUuid(uuid());
        inv.setVirtualRouterVmUuid(uuid());

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
