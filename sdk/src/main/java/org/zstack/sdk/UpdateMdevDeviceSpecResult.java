package org.zstack.sdk;

import org.zstack.sdk.MdevDeviceSpecInventory;

public class UpdateMdevDeviceSpecResult {
    public MdevDeviceSpecInventory inventory;
    public void setInventory(MdevDeviceSpecInventory inventory) {
        this.inventory = inventory;
    }
    public MdevDeviceSpecInventory getInventory() {
        return this.inventory;
    }

}
