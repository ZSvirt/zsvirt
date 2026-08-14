package org.zstack.sdk;

import org.zstack.sdk.VirtualRouterVmInventory;

public class UpdateVirtualRouterResult {
    public VirtualRouterVmInventory inventory;
    public void setInventory(VirtualRouterVmInventory inventory) {
        this.inventory = inventory;
    }
    public VirtualRouterVmInventory getInventory() {
        return this.inventory;
    }

}
