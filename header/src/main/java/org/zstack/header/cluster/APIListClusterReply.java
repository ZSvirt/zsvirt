package org.zstack.header.cluster;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListClusterReply extends APIReply {
    private List<ClusterInventory> inventories;

    public List<ClusterInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ClusterInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListClusterReply __example__() {
        APIListClusterReply reply = new APIListClusterReply();
        //deprecated
        return reply;
    }

}
