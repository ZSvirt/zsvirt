package org.zstack.sdk;

import org.zstack.sdk.KVMHostInventory;

public class UpdateHostIscsiInitiatorNameResult {
    public KVMHostInventory inventory;
    public void setInventory(KVMHostInventory inventory) {
        this.inventory = inventory;
    }
    public KVMHostInventory getInventory() {
        return this.inventory;
    }

}
