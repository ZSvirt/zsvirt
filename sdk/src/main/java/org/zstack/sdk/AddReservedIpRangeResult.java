package org.zstack.sdk;

import org.zstack.sdk.ReservedIpRangeInventory;

public class AddReservedIpRangeResult {
    public ReservedIpRangeInventory inventory;
    public void setInventory(ReservedIpRangeInventory inventory) {
        this.inventory = inventory;
    }
    public ReservedIpRangeInventory getInventory() {
        return this.inventory;
    }

}
