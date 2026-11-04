package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryL2VlanNetworkReply extends APIQueryReply {
    private List<L2VlanNetworkInventory> inventories;

    public List<L2VlanNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VlanNetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL2VlanNetworkReply __example__() {
        APIQueryL2VlanNetworkReply reply = new APIQueryL2VlanNetworkReply();
        reply.setInventories(list(L2VlanNetworkInventory.__example__()));
        return reply;
    }
}
