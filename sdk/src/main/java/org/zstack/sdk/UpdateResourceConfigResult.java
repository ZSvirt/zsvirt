package org.zstack.sdk;

import org.zstack.sdk.ResourceConfigInventory;

public class UpdateResourceConfigResult {
    public ResourceConfigInventory inventory;
    public void setInventory(ResourceConfigInventory inventory) {
        this.inventory = inventory;
    }
    public ResourceConfigInventory getInventory() {
        return this.inventory;
    }

}
