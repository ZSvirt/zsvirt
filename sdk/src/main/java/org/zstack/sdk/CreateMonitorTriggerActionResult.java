package org.zstack.sdk;

import org.zstack.sdk.MonitorTriggerActionInventory;

public class CreateMonitorTriggerActionResult {
    public MonitorTriggerActionInventory inventory;
    public void setInventory(MonitorTriggerActionInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorTriggerActionInventory getInventory() {
        return this.inventory;
    }

}
