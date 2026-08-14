package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateVO;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

@RestResponse(allTo = "inventory")
public class APIUpdateMetricRuleTemplateEvent extends APIEvent {
    private MetricRuleTemplateInventory inventory;

    public MetricRuleTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(MetricRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateMetricRuleTemplateEvent() {
    }

    public APIUpdateMetricRuleTemplateEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateMetricRuleTemplateEvent __example__() {
        APIUpdateMetricRuleTemplateEvent event = new APIUpdateMetricRuleTemplateEvent();
        APIUpdateMetricRuleTemplateMsg msg = APIUpdateMetricRuleTemplateMsg.__example__();
        MetricRuleTemplateInventory inventory = APIAddMetricRuleTemplateEvent.__example__().getInventory();
        inventory.setUuid(msg.getUuid());
        inventory.setName(msg.getName());
        inventory.setComparisonOperator(ComparisonOperator.valueOf(msg.getComparisonOperator()));
        inventory.setPeriod(msg.getPeriod());
        inventory.setThreshold(msg.getThreshold());
        inventory.setRepeatCount(msg.getRepeatCount());
        inventory.setEnableRecovery(msg.getEnableRecovery());
        inventory.setEmergencyLevel(EmergencyLevel.valueOf(msg.getEmergencyLevel()));
        event.setInventory(inventory);
        return event;
    }
}
