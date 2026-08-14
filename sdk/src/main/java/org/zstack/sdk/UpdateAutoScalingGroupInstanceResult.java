package org.zstack.sdk;

import org.zstack.sdk.AutoScalingGroupInstanceInventory;

public class UpdateAutoScalingGroupInstanceResult {
    public AutoScalingGroupInstanceInventory inventory;
    public void setInventory(AutoScalingGroupInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public AutoScalingGroupInstanceInventory getInventory() {
        return this.inventory;
    }

}
