package org.zstack.sdk.sns.platform.dingtalk;

import org.zstack.sdk.sns.platform.dingtalk.SNSDingTalkAtPersonInventory;

public class UpdateAtPersonOfDingTalkEndpointResult {
    public SNSDingTalkAtPersonInventory inventory;
    public void setInventory(SNSDingTalkAtPersonInventory inventory) {
        this.inventory = inventory;
    }
    public SNSDingTalkAtPersonInventory getInventory() {
        return this.inventory;
    }

}
