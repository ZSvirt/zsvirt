package org.zstack.monitoring;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryMonitorTriggerReply extends APIQueryReply {
    private List<MonitorTriggerInventory> inventories;

    public List<MonitorTriggerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorTriggerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryMonitorTriggerReply __example__() {
        APIQueryMonitorTriggerReply reply = new APIQueryMonitorTriggerReply();
        MonitorTriggerInventory inv = new MonitorTriggerInventory();
        inv.setUuid(uuid());
        inv.setDuration(60);
        inv.setExpression("vm.cpu.util{} > 100");
        inv.setTargetResourceUuid(uuid());
        inv.setName("VM1 CPU utilization trigger");
        inv.setState(MonitorTriggerState.Enabled.toString());
        inv.setStatus(MonitorTriggerStatus.OK.toString());
        reply.setInventories(asList(inv));
        return reply;
    }
}
