package org.zstack.header.vm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVmNicReply extends APIQueryReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVmNicReply __example__() {
        APIQueryVmNicReply reply = new APIQueryVmNicReply();


        return reply;
    }

}
