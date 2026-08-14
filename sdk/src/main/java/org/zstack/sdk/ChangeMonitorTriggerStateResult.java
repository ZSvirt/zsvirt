package org.zstack.sdk;

import org.zstack.sdk.MonitorTriggerInventory;

public class ChangeMonitorTriggerStateResult {
    public MonitorTriggerInventory inventory;
    public void setInventory(MonitorTriggerInventory inventory) {
        this.inventory = inventory;
    }
    public MonitorTriggerInventory getInventory() {
        return this.inventory;
    }

}
