package org.zstack.sdk;

import org.zstack.sdk.PciDeviceSpecInventory;

public class UpdatePciDeviceSpecResult {
    public PciDeviceSpecInventory inventory;
    public void setInventory(PciDeviceSpecInventory inventory) {
        this.inventory = inventory;
    }
    public PciDeviceSpecInventory getInventory() {
        return this.inventory;
    }

}
