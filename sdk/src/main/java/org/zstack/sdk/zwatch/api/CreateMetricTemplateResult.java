package org.zstack.sdk.zwatch.api;

import org.zstack.sdk.zwatch.metricpusher.MetricTemplateInventory;

public class CreateMetricTemplateResult {
    public MetricTemplateInventory inventory;
    public void setInventory(MetricTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public MetricTemplateInventory getInventory() {
        return this.inventory;
    }

}
