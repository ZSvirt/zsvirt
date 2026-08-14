package org.zstack.sdk.zwatch.monitorgroup.api;

import org.zstack.sdk.zwatch.monitorgroup.entity.EventRuleTemplateInventory;

public class UpdateEventRuleTemplateResult {
    public EventRuleTemplateInventory inventory;
    public void setInventory(EventRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public EventRuleTemplateInventory getInventory() {
        return this.inventory;
    }

}
