package org.zstack.sdk.sns.platform.dingtalk;

import org.zstack.sdk.sns.platform.dingtalk.SNSDingTalkEndpointInventory;

public class CreateSNSDingTalkEndpointResult {
    public SNSDingTalkEndpointInventory inventory;
    public void setInventory(SNSDingTalkEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSDingTalkEndpointInventory getInventory() {
        return this.inventory;
    }

}
