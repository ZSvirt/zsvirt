package org.zstack.sdk.zwatch.monitorgroup.api;

import org.zstack.sdk.zwatch.monitorgroup.entity.MonitorGroupInstanceInventory;

public class AddInstanceToMonitorGroupResult {
    public MonitorGroupInstanceInventory inventory;
    public void setInventory(MonitorGroupInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorGroupInstanceInventory getInventory() {
        return this.inventory;
    }

}
