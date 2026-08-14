package org.zstack.sdk.zwatch.monitorgroup.api;

import org.zstack.sdk.zwatch.monitorgroup.entity.MetricRuleTemplateInventory;

public class AddMetricRuleTemplateResult {
    public MetricRuleTemplateInventory inventory;
    public void setInventory(MetricRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public MetricRuleTemplateInventory getInventory() {
        return this.inventory;
    }

}
