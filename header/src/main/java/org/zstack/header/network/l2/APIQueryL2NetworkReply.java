package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryL2NetworkReply extends APIQueryReply {
    private List<L2NetworkInventory> inventories;

    public List<L2NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2NetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL2NetworkReply __example__() {
        APIQueryL2NetworkReply reply = new APIQueryL2NetworkReply();
        reply.setInventories(list(L2VlanNetworkInventory.__example__()));
        return reply;
    }

}
