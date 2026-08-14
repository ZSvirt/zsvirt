package org.zstack.header.vm;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListVmInstanceReply extends APIReply {
    private List<VmInstanceInventory> inventories;

    public List<VmInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListVmInstanceReply __example__() {
        APIListVmInstanceReply reply = new APIListVmInstanceReply();


        return reply;
    }

}
