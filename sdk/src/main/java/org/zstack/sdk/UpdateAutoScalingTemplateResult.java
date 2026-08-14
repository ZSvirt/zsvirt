package org.zstack.sdk;

import org.zstack.sdk.AutoScalingTemplateInventory;

public class UpdateAutoScalingTemplateResult {
    public AutoScalingTemplateInventory inventory;
    public void setInventory(AutoScalingTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingTemplateInventory getInventory() {
        return this.inventory;
    }

}
