package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SNSSmsReceiverInventory;

public class AddSNSSmsReceiverResult {
    public SNSSmsReceiverInventory inventory;
    public void setInventory(SNSSmsReceiverInventory inventory) {
        this.inventory = inventory;
    }
    public SNSSmsReceiverInventory getInventory() {
        return this.inventory;
    }

}
