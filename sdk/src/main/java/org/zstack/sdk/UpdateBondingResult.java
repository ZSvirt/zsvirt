package org.zstack.sdk;

import org.zstack.sdk.HostNetworkBondingInventory;

public class UpdateBondingResult {
    public HostNetworkBondingInventory inventory;
    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkBondingInventory getInventory() {
        return this.inventory;
    }

}
