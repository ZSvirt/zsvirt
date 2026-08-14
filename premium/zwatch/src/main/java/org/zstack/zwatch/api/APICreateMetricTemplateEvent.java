package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.metricpusher.MetricTemplateInventory;

@RestResponse(allTo = "inventory")
public class APICreateMetricTemplateEvent extends APIEvent {
    private MetricTemplateInventory inventory;

    public static APICreateMetricTemplateEvent __example__() {
        APICreateMetricTemplateEvent ret = new APICreateMetricTemplateEvent();
        MetricTemplateInventory inventory = MetricTemplateInventory.__example__();
        inventory.setUuid(uuid());
        inventory.setReceiverUuid(uuid());
        ret.setInventory(inventory);
        return ret;
    }

    public APICreateMetricTemplateEvent() {
    }

    public APICreateMetricTemplateEvent(String apiId) {
        super(apiId);
    }

    public MetricTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(MetricTemplateInventory inventory) {
        this.inventory = inventory;
    }
}
