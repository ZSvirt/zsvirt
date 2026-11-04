package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryIpRangeReply extends APIQueryReply {
    private List<IpRangeInventory> inventories;

    public List<IpRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IpRangeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryIpRangeReply __example__() {
        APIQueryIpRangeReply reply = new APIQueryIpRangeReply();
        reply.setInventories(list(IpRangeInventory.__example__()));
        return reply;
    }
}
