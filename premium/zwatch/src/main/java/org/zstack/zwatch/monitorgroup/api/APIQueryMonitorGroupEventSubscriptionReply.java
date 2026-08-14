package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupEventSubscriptionInventory;

import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMonitorGroupEventSubscriptionReply extends APIQueryReply {
    private List<MonitorGroupEventSubscriptionInventory> inventories;

    public static APIQueryMonitorGroupEventSubscriptionReply __example__() {
        APIQueryMonitorGroupEventSubscriptionReply ret = new APIQueryMonitorGroupEventSubscriptionReply();

        MonitorGroupEventSubscriptionInventory inventory = new MonitorGroupEventSubscriptionInventory();
        inventory.setGroupUuid(uuid());
        inventory.setEventRuleTemplateUuid(uuid());
        inventory.setEventSubscriptionUuid(uuid());
        inventory.setUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MonitorGroupEventSubscriptionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorGroupEventSubscriptionInventory> inventories) {
        this.inventories = inventories;
    }
}
