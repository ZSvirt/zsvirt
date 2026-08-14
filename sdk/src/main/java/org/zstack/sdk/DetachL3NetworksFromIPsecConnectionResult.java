package org.zstack.sdk;

import org.zstack.sdk.IPsecConnectionInventory;

public class DetachL3NetworksFromIPsecConnectionResult {
    public IPsecConnectionInventory inventory;
    public void setInventory(IPsecConnectionInventory inventory) {
        this.inventory = inventory;
    }
    public IPsecConnectionInventory getInventory() {
        return this.inventory;
    }

}
