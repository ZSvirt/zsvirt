package org.zstack.header.cluster;

import org.zstack.header.message.APIReply;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

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
