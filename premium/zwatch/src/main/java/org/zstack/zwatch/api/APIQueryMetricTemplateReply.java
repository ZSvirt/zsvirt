package org.zstack.zwatch.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.metricpusher.MetricTemplateInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMetricTemplateReply extends APIQueryReply {
    private List<MetricTemplateInventory> inventories;

    public static APIQueryMetricTemplateReply __example__() {
        APIQueryMetricTemplateReply ret = new APIQueryMetricTemplateReply();
        MetricTemplateInventory inventory = MetricTemplateInventory.__example__();
        inventory.setUuid(uuid());
        inventory.setReceiverUuid(uuid());
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MetricTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MetricTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}
