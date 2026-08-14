package org.zstack.sdk.sns.platform.feishu;

import org.zstack.sdk.sns.platform.feishu.SNSFeiShuEndpointInventory;

public class CreateSNSFeiShuEndpointResult {
    public SNSFeiShuEndpointInventory inventory;
    public void setInventory(SNSFeiShuEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSFeiShuEndpointInventory getInventory() {
        return this.inventory;
    }

}
