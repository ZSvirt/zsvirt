package org.zstack.monitoring;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/10.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateMonitorTriggerEvent extends APIEvent {
    private MonitorTriggerInventory inventory;

    public APIUpdateMonitorTriggerEvent() {
    }

    public APIUpdateMonitorTriggerEvent(String apiId) {
        super(apiId);
    }

    public MonitorTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTriggerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateMonitorTriggerEvent __example__() {
        APIUpdateMonitorTriggerEvent evt = new APIUpdateMonitorTriggerEvent();
        MonitorTriggerInventory inv = new MonitorTriggerInventory();
        inv.setUuid(uuid());
        inv.setDuration(60);
        inv.setExpression("vm.cpu.util{} > 100");
        inv.setTargetResourceUuid(uuid());
        inv.setName("VM1 CPU utilization trigger");
        inv.setState(MonitorTriggerState.Enabled.toString());
        inv.setStatus(MonitorTriggerStatus.OK.toString());
        evt.setInventory(inv);
        return evt;
    }
}
