package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryIpAddressReply extends APIQueryReply {
    private List<UsedIpInventory> inventories;

    public List<UsedIpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UsedIpInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryIpAddressReply __example__() {
        APIQueryIpAddressReply reply = new APIQueryIpAddressReply();
        reply.setInventories(list(UsedIpInventory.__example__()));
        return reply;
    }
}
