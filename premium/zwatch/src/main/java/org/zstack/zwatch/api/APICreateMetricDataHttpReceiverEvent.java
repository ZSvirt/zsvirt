package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.metricpusher.MetricDataHttpReceiverInventory;

@RestResponse(allTo = "inventory")
public class APICreateMetricDataHttpReceiverEvent extends APIEvent {
    private MetricDataHttpReceiverInventory inventory;

    public static APICreateMetricDataHttpReceiverEvent __example__() {
        APICreateMetricDataHttpReceiverEvent ret = new APICreateMetricDataHttpReceiverEvent();
        MetricDataHttpReceiverInventory inventory = MetricDataHttpReceiverInventory.__example__();
        inventory.setUuid(uuid());
        ret.setInventory(inventory);
        return ret;
    }

    public APICreateMetricDataHttpReceiverEvent() {
    }

    public APICreateMetricDataHttpReceiverEvent(String apiId) {
        super(apiId);
    }

    public MetricDataHttpReceiverInventory getInventory() {
        return inventory;
    }

    public void setInventory(MetricDataHttpReceiverInventory inventory) {
        this.inventory = inventory;
    }
}
