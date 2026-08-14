package org.zstack.sdk;

import org.zstack.sdk.VpcHaGroupInventory;

public class CreateVpcHaGroupResult {
    public VpcHaGroupInventory inventory;
    public void setInventory(VpcHaGroupInventory inventory) {
        this.inventory = inventory;
    }
    public VpcHaGroupInventory getInventory() {
        return this.inventory;
    }

}
