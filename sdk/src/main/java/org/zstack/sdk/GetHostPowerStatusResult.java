package org.zstack.sdk;

import org.zstack.sdk.HostIpmiInventory;

public class GetHostPowerStatusResult {
    public HostIpmiInventory inventory;
    public void setInventory(HostIpmiInventory inventory) {
        this.inventory = inventory;
    }
    public HostIpmiInventory getInventory() {
        return this.inventory;
    }

}
