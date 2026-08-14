package org.zstack.sdk.zwatch.monitorgroup.api;

import org.zstack.sdk.zwatch.monitorgroup.entity.MonitorGroupTemplateRefInventory;

public class ApplyMonitorTemplateToMonitorGroupResult {
    public MonitorGroupTemplateRefInventory inventory;
    public void setInventory(MonitorGroupTemplateRefInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorGroupTemplateRefInventory getInventory() {
        return this.inventory;
    }

}
