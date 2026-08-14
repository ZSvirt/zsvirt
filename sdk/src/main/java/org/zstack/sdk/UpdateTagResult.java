package org.zstack.sdk;

import org.zstack.sdk.TagPatternInventory;

public class UpdateTagResult {
    public TagPatternInventory inventory;
    public void setInventory(TagPatternInventory inventory) {
        this.inventory = inventory;
    }
    public TagPatternInventory getInventory() {
        return this.inventory;
    }

}
