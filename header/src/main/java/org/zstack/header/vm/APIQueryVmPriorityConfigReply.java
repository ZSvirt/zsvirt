package org.zstack.header.vm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVmPriorityConfigReply extends APIQueryReply {
    private List<VmPriorityConfigInventory> inventories;

    public List<VmPriorityConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmPriorityConfigInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVmPriorityConfigReply __example__() {
        APIQueryVmPriorityConfigReply reply = new APIQueryVmPriorityConfigReply();
        VmPriorityConfigInventory inv = new VmPriorityConfigInventory();
        inv.setUuid(uuid());
        inv.setCpuShares(2);
        inv.setLevel(VmPriorityLevel.Normal);
        inv.setOomScoreAdj(100);
        reply.setInventories(asList(inv));
        return reply;
    }

}
