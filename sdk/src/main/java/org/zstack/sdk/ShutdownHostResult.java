package org.zstack.sdk;

import org.zstack.sdk.HostInventory;

public class ShutdownHostResult {
    public HostInventory inventory;
    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
    public HostInventory getInventory() {
        return this.inventory;
    }

}
