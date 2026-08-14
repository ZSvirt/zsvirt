package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListL3NetworkReply extends APIReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListL3NetworkReply __example__() {
        APIListL3NetworkReply reply = new APIListL3NetworkReply();


        return reply;
    }

}
