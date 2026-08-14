package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupAlarmInventory;
import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMonitorGroupAlarmReply extends APIQueryReply {
    private List<MonitorGroupAlarmInventory> inventories;

    public static APIQueryMonitorGroupAlarmReply __example__() {
        APIQueryMonitorGroupAlarmReply ret = new APIQueryMonitorGroupAlarmReply();
        MonitorGroupAlarmInventory inventory = new MonitorGroupAlarmInventory();
        inventory.setUuid(uuid());
        inventory.setGroupUuid(uuid());
        inventory.setAlarmUuid(uuid());
        inventory.setMetricRuleTemplateUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MonitorGroupAlarmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorGroupAlarmInventory> inventories) {
        this.inventories = inventories;
    }
}
