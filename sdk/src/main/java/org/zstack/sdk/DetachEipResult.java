package org.zstack.sdk;

import org.zstack.sdk.EipInventory;

public class DetachEipResult {
    public EipInventory inventory;
    public void setInventory(EipInventory inventory) {
        this.inventory = inventory;
    }
    public EipInventory getInventory() {
        return this.inventory;
    }

}
