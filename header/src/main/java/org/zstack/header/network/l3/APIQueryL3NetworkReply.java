package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL3NetworkReply extends APIQueryReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL3NetworkReply __example__() {
        APIQueryL3NetworkReply reply = new APIQueryL3NetworkReply();
        L3NetworkInventory l3 = new L3NetworkInventory();

        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());

        reply.setInventories(Arrays.asList(l3));
        return reply;
    }

}
