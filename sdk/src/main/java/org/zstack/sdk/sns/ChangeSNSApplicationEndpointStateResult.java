package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SNSApplicationEndpointInventory;

public class ChangeSNSApplicationEndpointStateResult {
    public SNSApplicationEndpointInventory inventory;
    public void setInventory(SNSApplicationEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSApplicationEndpointInventory getInventory() {
        return this.inventory;
    }

}
