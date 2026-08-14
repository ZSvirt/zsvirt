package org.zstack.sdk;

import org.zstack.sdk.VmInstanceMdevDeviceSpecRefInventory;

public class AddMdevDeviceSpecToVmInstanceResult {
    public VmInstanceMdevDeviceSpecRefInventory inventory;
    public void setInventory(VmInstanceMdevDeviceSpecRefInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstanceMdevDeviceSpecRefInventory getInventory() {
        return this.inventory;
    }

}
