package org.zstack.sdk.sns.platform.http;

import org.zstack.sdk.sns.platform.http.SNSHttpEndpointInventory;

public class CreateSNSHttpEndpointResult {
    public SNSHttpEndpointInventory inventory;
    public void setInventory(SNSHttpEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSHttpEndpointInventory getInventory() {
        return this.inventory;
    }

}
