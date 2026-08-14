package org.zstack.zwatch.alarm.activealarm.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.zwatch.alarm.AlarmLabelInventory;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateInventory;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryActiveAlarmTemplateReply extends APIQueryReply {
    private List<ActiveAlarmTemplateInventory> inventories;

    public static APIQueryActiveAlarmTemplateReply __example__() {
        APIQueryActiveAlarmTemplateReply ret = new APIQueryActiveAlarmTemplateReply();
        ActiveAlarmTemplateInventory inventory = new ActiveAlarmTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setAlarmName("VM CPU Alarm");
        inventory.setComparisonOperator(ComparisonOperator.LessThanOrEqualTo);
        inventory.setPeriod(60);
        inventory.setNamespace("ZStack/VM");
        inventory.setMetricName(VmNamespace.CPUIdleUtilization.getName());
        inventory.setThreshold(30D);
        inventory.setEmergencyLevel(EmergencyLevel.Emergent);
        inventory.setRepeatInterval(60);
        inventory.setRepeatCount(-1);
        inventory.setLabels(JSONObjectUtil.toJsonString(asList(AlarmLabelInventory.__example__())));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<ActiveAlarmTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ActiveAlarmTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}
