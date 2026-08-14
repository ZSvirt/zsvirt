package org.zstack.header.resourceattribute.api;

import org.zstack.header.resourceattribute.entity.ResourceAttributeValueInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryResourceAttributeValueReply extends APIQueryReply {
    private List<ResourceAttributeValueInventory> inventories;

    public List<ResourceAttributeValueInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceAttributeValueInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryResourceAttributeValueReply __example__() {
        APIQueryResourceAttributeValueReply reply = new APIQueryResourceAttributeValueReply();
        reply.setInventories(list(ResourceAttributeValueInventory.__example__()));
        return reply;
    }
}
