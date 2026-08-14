package org.zstack.sdk.zwatch.alarm;

import org.zstack.sdk.zwatch.alarm.AlarmInventory;

public class UpdateAlarmResult {
    public AlarmInventory inventory;
    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
    public AlarmInventory getInventory() {
        return this.inventory;
    }

}
