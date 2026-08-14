package org.zstack.sdk.zwatch.monitorgroup.api;

import org.zstack.sdk.zwatch.monitorgroup.entity.MonitorGroupInventory;

public class CreateMonitorGroupResult {
    public MonitorGroupInventory inventory;
    public void setInventory(MonitorGroupInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorGroupInventory getInventory() {
        return this.inventory;
    }

}
