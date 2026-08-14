package org.zstack.sdk;

import org.zstack.sdk.ScsiLunInventory;

public class DetachScsiLunFromHostResult {
    public ScsiLunInventory inventory;
    public void setInventory(ScsiLunInventory inventory) {
        this.inventory = inventory;
    }
    public ScsiLunInventory getInventory() {
        return this.inventory;
    }

}
