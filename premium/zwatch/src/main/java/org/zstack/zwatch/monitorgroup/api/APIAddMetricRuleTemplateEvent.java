package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIAddMetricRuleTemplateEvent extends APIEvent {

    private MetricRuleTemplateInventory inventory;

    public static APIAddMetricRuleTemplateEvent __example__() {
        APIAddMetricRuleTemplateEvent ret = new APIAddMetricRuleTemplateEvent();
        MetricRuleTemplateInventory inventory = new MetricRuleTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setNamespace("ZStack/VM");
        inventory.setName("vm-cpu");
        inventory.setPeriod(60);
        inventory.setThreshold(80d);
        inventory.setEnableRecovery(true);
        inventory.setMetricName(VmNamespace.CPUUsedUtilization.getName());
        inventory.setMonitorTemplateUuid(uuid());
        inventory.setComparisonOperator(ComparisonOperator.GreaterThan);
        inventory.setEmergencyLevel(EmergencyLevel.Normal);
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.setInventory(inventory);
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
