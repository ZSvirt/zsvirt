package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SNSApplicationPlatformInventory;

public class UpdateSNSApplicationPlatformResult {
    public SNSApplicationPlatformInventory inventory;
    public void setInventory(SNSApplicationPlatformInventory inventory) {
        this.inventory = inventory;
    }
    public SNSApplicationPlatformInventory getInventory() {
        return this.inventory;
    }

}
