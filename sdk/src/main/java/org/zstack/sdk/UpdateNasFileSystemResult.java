package org.zstack.sdk;

import org.zstack.sdk.NasFileSystemInventory;

public class UpdateNasFileSystemResult {
    public NasFileSystemInventory inventory;
    public void setInventory(NasFileSystemInventory inventory) {
        this.inventory = inventory;
    }
    public NasFileSystemInventory getInventory() {
        return this.inventory;
    }

}
