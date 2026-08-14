package org.zstack.sdk.zwatch.monitorgroup.api;

import org.zstack.sdk.zwatch.monitorgroup.entity.MonitorTemplateInventory;

public class CreateMonitorTemplateResult {
    public MonitorTemplateInventory inventory;
    public void setInventory(MonitorTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorTemplateInventory getInventory() {
        return this.inventory;
    }

}
