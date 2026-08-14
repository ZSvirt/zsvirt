package org.zstack.sdk.sns.platform.wecom;

import org.zstack.sdk.sns.platform.wecom.SNSWeComEndpointInventory;

public class CreateSNSWeComEndpointResult {
    public SNSWeComEndpointInventory inventory;
    public void setInventory(SNSWeComEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSWeComEndpointInventory getInventory() {
        return this.inventory;
    }

}
