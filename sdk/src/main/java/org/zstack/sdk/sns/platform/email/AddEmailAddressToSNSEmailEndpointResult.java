package org.zstack.sdk.sns.platform.email;

import org.zstack.sdk.sns.platform.email.SNSEmailAddressInventory;

public class AddEmailAddressToSNSEmailEndpointResult {
    public SNSEmailAddressInventory inventory;
    public void setInventory(SNSEmailAddressInventory inventory) {
        this.inventory = inventory;
    }
    public SNSEmailAddressInventory getInventory() {
        return this.inventory;
    }

}
