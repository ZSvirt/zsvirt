package org.zstack.sdk.sns;

import org.zstack.sdk.sns.SNSTopicInventory;

public class UpdateSNSTopicResult {
    public SNSTopicInventory inventory;
    public void setInventory(SNSTopicInventory inventory) {
        this.inventory = inventory;
    }
    public SNSTopicInventory getInventory() {
        return this.inventory;
    }

}
