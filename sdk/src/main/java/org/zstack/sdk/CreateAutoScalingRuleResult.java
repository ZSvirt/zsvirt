package org.zstack.sdk;

import org.zstack.sdk.AutoScalingRuleInventory;

public class CreateAutoScalingRuleResult {
    public AutoScalingRuleInventory inventory;
    public void setInventory(AutoScalingRuleInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingRuleInventory getInventory() {
        return this.inventory;
    }

}
