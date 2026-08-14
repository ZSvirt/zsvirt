package org.zstack.sdk;

import org.zstack.sdk.MonitorTriggerInventory;

public class UpdateMonitorTriggerResult {
    public MonitorTriggerInventory inventory;
    public void setInventory(MonitorTriggerInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorTriggerInventory getInventory() {
        return this.inventory;
    }

}
