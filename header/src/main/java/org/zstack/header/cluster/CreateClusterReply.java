package org.zstack.header.cluster;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.message.MessageReply;

public class CreateClusterReply extends MessageReply {
    private ClusterInventory inventory;

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
}
