package org.zstack.allocator.domain.api;

import org.zstack.allocator.domain.entity.AllocatorDomainInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryAllocatorDomainReply extends APIQueryReply {
    private List<AllocatorDomainInventory> inventories;

    public List<AllocatorDomainInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AllocatorDomainInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAllocatorDomainReply __example__() {
        APIQueryAllocatorDomainReply reply = new APIQueryAllocatorDomainReply();
        reply.setInventories(list(AllocatorDomainInventory.__example__()));
        return reply;
    }
}
