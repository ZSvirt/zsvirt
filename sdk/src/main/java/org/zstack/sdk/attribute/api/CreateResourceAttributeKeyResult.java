package org.zstack.sdk.attribute.api;

import org.zstack.sdk.attribute.entity.ResourceAttributeKeyInventory;

public class CreateResourceAttributeKeyResult {
    public ResourceAttributeKeyInventory inventory;
    public void setInventory(ResourceAttributeKeyInventory inventory) {
        this.inventory = inventory;
    }
    public ResourceAttributeKeyInventory getInventory() {
        return this.inventory;
    }

}
