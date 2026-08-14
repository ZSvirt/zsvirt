package org.zstack.sdk.zwatch.alarm.activealarm.api;

import org.zstack.sdk.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateInventory;

public class UpdateActiveAlarmTemplateResult {
    public ActiveAlarmTemplateInventory inventory;
    public void setInventory(ActiveAlarmTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public ActiveAlarmTemplateInventory getInventory() {
        return this.inventory;
    }

}
