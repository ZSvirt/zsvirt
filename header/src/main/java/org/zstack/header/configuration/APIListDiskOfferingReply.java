package org.zstack.header.configuration;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListDiskOfferingReply extends APIReply {
    private List<DiskOfferingInventory> inventories;

    public List<DiskOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DiskOfferingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListDiskOfferingReply __example__() {
        APIListDiskOfferingReply reply = new APIListDiskOfferingReply();


        return reply;
    }

}
