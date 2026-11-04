package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory;

@RestResponse(allTo = "inventory")
public class APIAddMetricRuleTemplateEvent extends APIEvent {

    private MetricRuleTemplateInventory inventory;

    public static APIAddMetricRuleTemplateEvent __example__() {
        APIAddMetricRuleTemplateEvent ret = new APIAddMetricRuleTemplateEvent();
        ret.setInventory(MetricRuleTemplateInventory.__example__());
        return ret;
    }

    public APIAddMetricRuleTemplateEvent() {
    }

    public MetricRuleTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(MetricRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddMetricRuleTemplateEvent(String apiId) {
        super(apiId);
    }
}
