package org.zstack.sdk;

import org.zstack.sdk.VmInstancePciDeviceSpecRefInventory;

public class AddPciDeviceSpecToVmInstanceResult {
    public VmInstancePciDeviceSpecRefInventory inventory;
    public void setInventory(VmInstancePciDeviceSpecRefInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstancePciDeviceSpecRefInventory getInventory() {
        return this.inventory;
    }

}
