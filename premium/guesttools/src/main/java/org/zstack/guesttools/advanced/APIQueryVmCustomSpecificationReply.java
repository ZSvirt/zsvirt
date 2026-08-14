package org.zstack.guesttools.advanced;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVmCustomSpecificationReply extends APIQueryReply {
    private List<VmCustomSpecificationInventory> inventories;

    public List<VmCustomSpecificationInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmCustomSpecificationInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmCustomSpecificationReply __example__() {
        APIQueryVmCustomSpecificationReply reply = new APIQueryVmCustomSpecificationReply();
        reply.setInventories(Collections.singletonList(VmCustomSpecificationInventory.__example__()));
        return reply;
    }
}
