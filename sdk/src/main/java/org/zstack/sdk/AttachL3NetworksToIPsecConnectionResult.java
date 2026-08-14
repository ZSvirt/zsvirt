package org.zstack.sdk;

import org.zstack.sdk.IPsecConnectionInventory;

public class AttachL3NetworksToIPsecConnectionResult {
    public IPsecConnectionInventory inventory;
    public void setInventory(IPsecConnectionInventory inventory) {
        this.inventory = inventory;
    }
    public IPsecConnectionInventory getInventory() {
        return this.inventory;
    }

}
