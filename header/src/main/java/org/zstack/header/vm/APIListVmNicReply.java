package org.zstack.header.vm;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListVmNicReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListVmNicReply __example__() {
        APIListVmNicReply reply = new APIListVmNicReply();


        return reply;
    }

}
