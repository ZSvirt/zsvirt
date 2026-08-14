package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListL2VlanNetworkReply extends APIReply {
    private List<L2VlanNetworkInventory> inventories;

    public List<L2VlanNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VlanNetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListL2VlanNetworkReply __example__() {
        APIListL2VlanNetworkReply reply = new APIListL2VlanNetworkReply();


        return reply;
    }

}
