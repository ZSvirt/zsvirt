package org.zstack.sdk.sns.platform.wecom;

import org.zstack.sdk.sns.platform.wecom.SNSWeComAtPersonInventory;

public class UpdateAtPersonOfWeComEndpointResult {
    public SNSWeComAtPersonInventory inventory;
    public void setInventory(SNSWeComAtPersonInventory inventory) {
        this.inventory = inventory;
    }
    public SNSWeComAtPersonInventory getInventory() {
        return this.inventory;
    }

}
