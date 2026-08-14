package org.zstack.sdk;

import org.zstack.sdk.PriceInventory;

public class UpdateResourcePriceResult {
    public PriceInventory inventory;
    public void setInventory(PriceInventory inventory) {
        this.inventory = inventory;
    }
    public PriceInventory getInventory() {
        return this.inventory;
    }

}
