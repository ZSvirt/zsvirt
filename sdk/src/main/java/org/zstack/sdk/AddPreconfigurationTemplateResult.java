package org.zstack.sdk;

import org.zstack.sdk.PreconfigurationTemplateInventory;

public class AddPreconfigurationTemplateResult {
    public PreconfigurationTemplateInventory inventory;
    public void setInventory(PreconfigurationTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public PreconfigurationTemplateInventory getInventory() {
        return this.inventory;
    }

}
