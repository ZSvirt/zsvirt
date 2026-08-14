package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListL2NetworkReply extends APIReply {
    private List<L2NetworkInventory> inventories;

    public List<L2NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2NetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListL2NetworkReply __example__() {
        APIListL2NetworkReply reply = new APIListL2NetworkReply();


        return reply;
    }

}
