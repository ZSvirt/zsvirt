package org.zstack.sdk;

import org.zstack.sdk.HaStrategyConditionInventory;

public class UpdateHaStrategyConditionResult {
    public HaStrategyConditionInventory inventory;
    public void setInventory(HaStrategyConditionInventory inventory) {
        this.inventory = inventory;
    }
    public HaStrategyConditionInventory getInventory() {
        return this.inventory;
    }

}
