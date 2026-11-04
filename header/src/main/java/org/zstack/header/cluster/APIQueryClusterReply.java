package org.zstack.header.cluster;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryClusterReply extends APIQueryReply {
    private List<ClusterInventory> inventories;

    public List<ClusterInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ClusterInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryClusterReply __example__() {
        APIQueryClusterReply reply = new APIQueryClusterReply();
        reply.setInventories(list(ClusterInventory.__example__()));
        return reply;
    }
}
