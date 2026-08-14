package org.zstack.sdk;

import org.zstack.sdk.IpRangeInventory;

public class AddIpRangeResult {
    public IpRangeInventory inventory;
    public void setInventory(IpRangeInventory inventory) {
        this.inventory = inventory;
    }
    public IpRangeInventory getInventory() {
        return this.inventory;
    }

}
