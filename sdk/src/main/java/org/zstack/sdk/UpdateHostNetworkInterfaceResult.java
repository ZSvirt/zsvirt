package org.zstack.sdk;

import org.zstack.sdk.HostNetworkInterfaceInventory;

public class UpdateHostNetworkInterfaceResult {
    public HostNetworkInterfaceInventory inventory;
    public void setInventory(HostNetworkInterfaceInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkInterfaceInventory getInventory() {
        return this.inventory;
    }

}
