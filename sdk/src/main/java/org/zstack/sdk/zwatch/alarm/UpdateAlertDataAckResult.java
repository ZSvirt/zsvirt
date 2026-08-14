package org.zstack.sdk.zwatch.alarm;

import org.zstack.sdk.zwatch.alarm.AlertDataAckInventory;

public class UpdateAlertDataAckResult {
    public AlertDataAckInventory inventory;
    public void setInventory(AlertDataAckInventory inventory) {
        this.inventory = inventory;
    }
    public AlertDataAckInventory getInventory() {
        return this.inventory;
    }

}
