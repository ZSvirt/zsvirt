package org.zstack.sdk;

import org.zstack.sdk.CasClientInventory;

public class UpdateCasClientResult {
    public CasClientInventory inventory;
    public void setInventory(CasClientInventory inventory) {
        this.inventory = inventory;
    }
    public CasClientInventory getInventory() {
        return this.inventory;
    }

}
