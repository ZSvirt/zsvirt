package org.zstack.header.configuration;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListInstanceOfferingReply extends APIReply {
    private List<InstanceOfferingInventory> inventories;

    public List<InstanceOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InstanceOfferingInventory> inventories) {
        this.inventories = inventories;
    }

 
    public static APIListInstanceOfferingReply __example__() {
        APIListInstanceOfferingReply reply = new APIListInstanceOfferingReply();


        return reply;
    }

}
