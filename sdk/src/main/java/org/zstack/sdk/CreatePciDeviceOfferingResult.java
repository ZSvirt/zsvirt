package org.zstack.sdk;

import org.zstack.sdk.PciDeviceOfferingInventory;

public class CreatePciDeviceOfferingResult {
    public PciDeviceOfferingInventory inventory;
    public void setInventory(PciDeviceOfferingInventory inventory) {
        this.inventory = inventory;
    }
    public PciDeviceOfferingInventory getInventory() {
        return this.inventory;
    }

}
