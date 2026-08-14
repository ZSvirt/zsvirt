package org.zstack.sdk;

import org.zstack.sdk.SystemTagInventory;

public class CreateSystemTagResult {
    public SystemTagInventory inventory;
    public void setInventory(SystemTagInventory inventory) {
        this.inventory = inventory;
    }
    public SystemTagInventory getInventory() {
        return this.inventory;
    }

}
