package org.zstack.header.vm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVmSchedHistoryReply extends APIQueryReply {
    private List<VmSchedHistoryInventory> inventories;

    public List<VmSchedHistoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmSchedHistoryInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmSchedHistoryReply __example__() {
        APIQueryVmSchedHistoryReply reply = new APIQueryVmSchedHistoryReply();
        VmSchedHistoryInventory inventory = new VmSchedHistoryInventory();
        reply.setInventories(Collections.singletonList(inventory));
        return reply;
    }
}
