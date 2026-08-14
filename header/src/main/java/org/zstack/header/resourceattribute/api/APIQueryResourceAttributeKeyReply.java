package org.zstack.header.resourceattribute.api;

import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryResourceAttributeKeyReply extends APIQueryReply {
    private List<ResourceAttributeKeyInventory> inventories;

    public List<ResourceAttributeKeyInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ResourceAttributeKeyInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryResourceAttributeKeyReply __example__() {
        APIQueryResourceAttributeKeyReply reply = new APIQueryResourceAttributeKeyReply();
        reply.setInventories(list(ResourceAttributeKeyInventory.__example__()));
        return reply;
    }
}
