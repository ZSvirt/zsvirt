package org.zstack.sdk;

import org.zstack.sdk.L3NetworkInventory;

public class AddDnsToL3NetworkResult {
    public L3NetworkInventory inventory;
    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
    public L3NetworkInventory getInventory() {
        return this.inventory;
    }

}
