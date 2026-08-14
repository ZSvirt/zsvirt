package org.zstack.sdk;

import org.zstack.sdk.StackTemplateInventory;

public class UpdateStackTemplateResult {
    public StackTemplateInventory inventory;
    public void setInventory(StackTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public StackTemplateInventory getInventory() {
        return this.inventory;
    }

}
