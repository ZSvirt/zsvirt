package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryAddressPoolReply extends APIQueryReply {
    private List<AddressPoolInventory> inventories;

    public List<AddressPoolInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AddressPoolInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAddressPoolReply __example__() {
        APIQueryAddressPoolReply reply = new APIQueryAddressPoolReply();
        reply.setInventories(list(AddressPoolInventory.__example__()));
        return reply;
    }
}
