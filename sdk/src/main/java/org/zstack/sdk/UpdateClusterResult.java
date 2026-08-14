package org.zstack.sdk;

import org.zstack.sdk.ClusterInventory;

public class UpdateClusterResult {
    public ClusterInventory inventory;
    public void setInventory(ClusterInventory inventory) {
        this.inventory = inventory;
    }
    public ClusterInventory getInventory() {
        return this.inventory;
    }

}
