package org.zstack.sdk;

import org.zstack.sdk.DiskOfferingInventory;

public class CreateDiskOfferingResult {
    public DiskOfferingInventory inventory;
    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }
    public DiskOfferingInventory getInventory() {
        return this.inventory;
    }

}
