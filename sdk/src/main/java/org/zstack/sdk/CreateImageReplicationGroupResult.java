package org.zstack.sdk;

import org.zstack.sdk.ImageReplicationGroupInventory;

public class CreateImageReplicationGroupResult {
    public ImageReplicationGroupInventory inventory;
    public void setInventory(ImageReplicationGroupInventory inventory) {
        this.inventory = inventory;
    }
    public ImageReplicationGroupInventory getInventory() {
        return this.inventory;
    }

}
