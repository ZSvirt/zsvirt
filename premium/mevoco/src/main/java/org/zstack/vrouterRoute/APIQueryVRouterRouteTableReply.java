package org.zstack.vrouterRoute;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVRouterRouteTableReply extends APIQueryReply {
    private List<VRouterRouteTableInventory> inventories;

    public List<VRouterRouteTableInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VRouterRouteTableInventory> inventories) {
        this.inventories = inventories;
    }
}
