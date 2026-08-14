package org.zstack.sdk;

import org.zstack.sdk.AutoScalingGroupInventory;

public class AttachAutoScalingTemplateToGroupResult {
    public AutoScalingGroupInventory inventory;
    public void setInventory(AutoScalingGroupInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingGroupInventory getInventory() {
        return this.inventory;
    }

}
