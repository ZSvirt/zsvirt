package org.zstack.sdk;

import org.zstack.sdk.MdevDeviceInventory;

public class UpdateMdevDeviceResult {
    public MdevDeviceInventory inventory;
    public void setInventory(MdevDeviceInventory inventory) {
        this.inventory = inventory;
    }
    public MdevDeviceInventory getInventory() {
        return this.inventory;
    }

}
