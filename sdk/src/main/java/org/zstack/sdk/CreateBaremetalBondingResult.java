package org.zstack.sdk;

import org.zstack.sdk.BaremetalBondingInventory;

public class CreateBaremetalBondingResult {
    public BaremetalBondingInventory inventory;
    public void setInventory(BaremetalBondingInventory inventory) {
        this.inventory = inventory;
    }
    public BaremetalBondingInventory getInventory() {
        return this.inventory;
    }

}
