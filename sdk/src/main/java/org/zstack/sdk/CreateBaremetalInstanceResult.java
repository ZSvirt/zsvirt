package org.zstack.sdk;

import org.zstack.sdk.BaremetalInstanceInventory;

public class CreateBaremetalInstanceResult {
    public BaremetalInstanceInventory inventory;
    public void setInventory(BaremetalInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public BaremetalInstanceInventory getInventory() {
        return this.inventory;
    }

}
