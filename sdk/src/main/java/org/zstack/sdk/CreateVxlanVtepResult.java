package org.zstack.sdk;

import org.zstack.sdk.VtepInventory;

public class CreateVxlanVtepResult {
    public VtepInventory inventory;
    public void setInventory(VtepInventory inventory) {
        this.inventory = inventory;
    }
    public VtepInventory getInventory() {
        return this.inventory;
    }

}
