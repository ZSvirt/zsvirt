package org.zstack.sdk;

import org.zstack.sdk.InstanceOfferingInventory;

public class ChangeInstanceOfferingStateResult {
    public InstanceOfferingInventory inventory;
    public void setInventory(InstanceOfferingInventory inventory) {
        this.inventory = inventory;
    }
    public InstanceOfferingInventory getInventory() {
        return this.inventory;
    }

}
