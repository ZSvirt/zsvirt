package org.zstack.monitoring.actions;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.awt.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestResponse(allTo = "inventory")
public class APICreateMonitorTriggerActionEvent extends APIEvent {
    private MonitorTriggerActionInventory inventory;

    public APICreateMonitorTriggerActionEvent() {
    }

    public APICreateMonitorTriggerActionEvent(String apiId) {
        super(apiId);
    }

    public MonitorTriggerActionInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTriggerActionInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateMonitorTriggerActionEvent __example__() {
        APICreateMonitorTriggerActionEvent evt = new APICreateMonitorTriggerActionEvent();

        MonitorTriggerActionInventory inv = new MonitorTriggerActionInventory();
        inv.setUuid(uuid());
        inv.setName("email");
        inv.setState(MonitorTriggerActionState.Enabled.toString());
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setInventory(inv);
        return evt;
    }
}
