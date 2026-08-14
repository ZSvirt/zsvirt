package org.zstack.sdk.zwatch.alarm.sns;

import org.zstack.sdk.zwatch.alarm.sns.SNSTextTemplateInventory;

public class UpdateSNSTextTemplateResult {
    public SNSTextTemplateInventory inventory;
    public void setInventory(SNSTextTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public SNSTextTemplateInventory getInventory() {
        return this.inventory;
    }

}
