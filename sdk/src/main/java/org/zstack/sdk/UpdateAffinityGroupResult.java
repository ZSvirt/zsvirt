package org.zstack.sdk;

import org.zstack.sdk.AffinityGroupInventory;

public class UpdateAffinityGroupResult {
    public AffinityGroupInventory inventory;
    public void setInventory(AffinityGroupInventory inventory) {
        this.inventory = inventory;
    }
    public AffinityGroupInventory getInventory() {
        return this.inventory;
    }

}
