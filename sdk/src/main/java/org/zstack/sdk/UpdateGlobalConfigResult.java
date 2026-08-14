package org.zstack.sdk;

import org.zstack.sdk.GlobalConfigInventory;

public class UpdateGlobalConfigResult {
    public GlobalConfigInventory inventory;
    public void setInventory(GlobalConfigInventory inventory) {
        this.inventory = inventory;
    }
    public GlobalConfigInventory getInventory() {
        return this.inventory;
    }

}
