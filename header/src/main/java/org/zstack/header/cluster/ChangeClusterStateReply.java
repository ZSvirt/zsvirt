package org.zstack.header.cluster;

import org.zstack.header.message.MessageReply;

public class ChangeClusterStateReply extends MessageReply {
    private ClusterInventory inventory;

    public ChangeClusterStateReply() {
    }

    public ClusterInventory getInventory() {
        return inventory;
    }

    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
}
