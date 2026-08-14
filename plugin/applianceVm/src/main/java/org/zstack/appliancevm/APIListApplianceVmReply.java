package org.zstack.appliancevm;

import org.zstack.header.message.APIReply;

import java.util.List;

/**
 */
public class APIListApplianceVmReply extends APIReply {
    private List<ApplianceVmInventory> inventories;

    public List<ApplianceVmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ApplianceVmInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListApplianceVmReply __example__() {
        APIListApplianceVmReply reply = new APIListApplianceVmReply();


        return reply;
    }

}
