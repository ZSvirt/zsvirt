package org.zstack.monitoring.actions;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryMonitorTriggerActionReply extends APIQueryReply {
    private List<MonitorTriggerActionInventory> inventories;

    public List<MonitorTriggerActionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorTriggerActionInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryMonitorTriggerActionReply __example__() {
        APIQueryMonitorTriggerActionReply reply = new APIQueryMonitorTriggerActionReply();
        MonitorTriggerActionInventory inv = new MonitorTriggerActionInventory();
        inv.setUuid(uuid());
        inv.setName("email");
        inv.setState(MonitorTriggerActionState.Enabled.toString());
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(inv));
        return reply;
    }
}
