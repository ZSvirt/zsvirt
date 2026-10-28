package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryInstanceOfferingReply extends APIQueryReply {
    private List<InstanceOfferingInventory> inventories;

    public List<InstanceOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InstanceOfferingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryInstanceOfferingReply __example__() {
        APIQueryInstanceOfferingReply reply = new APIQueryInstanceOfferingReply();
        reply.setInventories(list(InstanceOfferingInventory.__example__()));
        return reply;
    }
}
