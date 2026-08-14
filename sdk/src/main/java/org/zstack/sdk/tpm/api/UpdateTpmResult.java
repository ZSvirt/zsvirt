package org.zstack.sdk.tpm.api;

import org.zstack.sdk.tpm.entity.TpmInventory;

public class UpdateTpmResult {
    public TpmInventory inventory;
    public void setInventory(TpmInventory inventory) {
        this.inventory = inventory;
    }
    public TpmInventory getInventory() {
        return this.inventory;
    }

}
