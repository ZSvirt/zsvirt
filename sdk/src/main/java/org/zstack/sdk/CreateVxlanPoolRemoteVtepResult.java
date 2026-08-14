package org.zstack.sdk;

import org.zstack.sdk.RemoteVtepInventory;

public class CreateVxlanPoolRemoteVtepResult {
    public RemoteVtepInventory inventory;
    public void setInventory(RemoteVtepInventory inventory) {
        this.inventory = inventory;
    }
    public RemoteVtepInventory getInventory() {
        return this.inventory;
    }

}
