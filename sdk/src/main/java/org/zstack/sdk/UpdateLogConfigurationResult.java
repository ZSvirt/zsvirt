package org.zstack.sdk;

import org.zstack.sdk.JsonLabelInventory;

public class UpdateLogConfigurationResult {
    public JsonLabelInventory inventory;
    public void setInventory(JsonLabelInventory inventory) {
        this.inventory = inventory;
    }
    public JsonLabelInventory getInventory() {
        return this.inventory;
    }

}
