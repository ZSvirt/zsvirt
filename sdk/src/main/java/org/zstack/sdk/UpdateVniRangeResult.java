package org.zstack.sdk;

import org.zstack.sdk.VniRangeInventory;

public class UpdateVniRangeResult {
    public VniRangeInventory inventory;
    public void setInventory(VniRangeInventory inventory) {
        this.inventory = inventory;
    }
    public VniRangeInventory getInventory() {
        return this.inventory;
    }

}
