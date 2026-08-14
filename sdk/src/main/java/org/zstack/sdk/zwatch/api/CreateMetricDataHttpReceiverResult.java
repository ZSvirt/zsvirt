package org.zstack.sdk.zwatch.api;

import org.zstack.sdk.zwatch.metricpusher.MetricDataHttpReceiverInventory;

public class CreateMetricDataHttpReceiverResult {
    public MetricDataHttpReceiverInventory inventory;
    public void setInventory(MetricDataHttpReceiverInventory inventory) {
        this.inventory = inventory;
    }
    public MetricDataHttpReceiverInventory getInventory() {
        return this.inventory;
    }

}
