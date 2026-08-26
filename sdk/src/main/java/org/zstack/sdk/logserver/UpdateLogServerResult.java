package org.zstack.sdk.logserver;

import org.zstack.sdk.logserver.LogServerInventory;

public class UpdateLogServerResult {
    public LogServerInventory inventory;
    public void setInventory(LogServerInventory inventory) {
        this.inventory = inventory;
    }
    public LogServerInventory getInventory() {
        return this.inventory;
    }

}
