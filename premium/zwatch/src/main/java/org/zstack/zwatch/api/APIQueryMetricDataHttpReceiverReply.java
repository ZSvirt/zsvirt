package org.zstack.zwatch.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.metricpusher.MetricDataHttpReceiverInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMetricDataHttpReceiverReply extends APIQueryReply {
    private List<MetricDataHttpReceiverInventory> inventories;

    public static APIQueryMetricDataHttpReceiverReply __example__() {
        APIQueryMetricDataHttpReceiverReply ret = new APIQueryMetricDataHttpReceiverReply();
        MetricDataHttpReceiverInventory inventory = MetricDataHttpReceiverInventory.__example__();
        inventory.setUuid(uuid());
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MetricDataHttpReceiverInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MetricDataHttpReceiverInventory> inventories) {
        this.inventories = inventories;
    }
}
