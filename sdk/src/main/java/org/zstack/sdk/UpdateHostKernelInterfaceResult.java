package org.zstack.sdk;

import org.zstack.sdk.HostKernelInterfaceInventory;

public class UpdateHostKernelInterfaceResult {
    public HostKernelInterfaceInventory inventory;
    public void setInventory(HostKernelInterfaceInventory inventory) {
        this.inventory = inventory;
    }
    public HostKernelInterfaceInventory getInventory() {
        return this.inventory;
    }

}
