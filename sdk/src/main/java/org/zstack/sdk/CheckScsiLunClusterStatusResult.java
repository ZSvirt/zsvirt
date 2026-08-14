package org.zstack.sdk;

import org.zstack.sdk.ScsiLunClusterStatusInventory;

public class CheckScsiLunClusterStatusResult {
    public ScsiLunClusterStatusInventory inventory;
    public void setInventory(ScsiLunClusterStatusInventory inventory) {
        this.inventory = inventory;
    }
    public ScsiLunClusterStatusInventory getInventory() {
        return this.inventory;
    }

}
