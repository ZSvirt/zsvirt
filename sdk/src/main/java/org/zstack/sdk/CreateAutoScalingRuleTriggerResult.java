package org.zstack.sdk;

import org.zstack.sdk.AutoScalingRuleTriggerInventory;

public class CreateAutoScalingRuleTriggerResult {
    public AutoScalingRuleTriggerInventory inventory;
    public void setInventory(AutoScalingRuleTriggerInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingRuleTriggerInventory getInventory() {
        return this.inventory;
    }

}
