package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMetricRuleTemplateReply extends APIQueryReply {
    private List<MetricRuleTemplateInventory> inventories;

    public static APIQueryMetricRuleTemplateReply __example__() {
        APIQueryMetricRuleTemplateReply ret = new APIQueryMetricRuleTemplateReply();
        MetricRuleTemplateInventory inventory = APIAddMetricRuleTemplateEvent.__example__().getInventory();
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MetricRuleTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MetricRuleTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}
