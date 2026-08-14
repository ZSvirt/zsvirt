package org.zstack.sdk.zwatch.thirdparty.api;

import org.zstack.sdk.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;

public class AddThirdpartyPlatformResult {
    public ThirdpartyPlatformInventory inventory;
    public void setInventory(ThirdpartyPlatformInventory inventory) {
        this.inventory = inventory;
    }
    public ThirdpartyPlatformInventory getInventory() {
        return this.inventory;
    }

}
