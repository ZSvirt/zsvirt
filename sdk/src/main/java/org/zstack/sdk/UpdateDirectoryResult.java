package org.zstack.sdk;

import org.zstack.sdk.DirectoryInventory;

public class UpdateDirectoryResult {
    public DirectoryInventory inventory;
    public void setInventory(DirectoryInventory inventory) {
        this.inventory = inventory;
    }
    public DirectoryInventory getInventory() {
        return this.inventory;
    }

}
