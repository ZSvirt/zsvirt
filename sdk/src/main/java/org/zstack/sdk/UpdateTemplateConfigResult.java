package org.zstack.sdk;

import org.zstack.sdk.TemplateConfigInventory;

public class UpdateTemplateConfigResult {
    public TemplateConfigInventory inventory;
    public void setInventory(TemplateConfigInventory inventory) {
        this.inventory = inventory;
    }
    public TemplateConfigInventory getInventory() {
        return this.inventory;
    }

}
