package org.zstack.monitoring.actions;

import org.zstack.core.thread.SyncTask;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by lining on 2017/08/23.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateMonitorTriggerActionEvent extends APIEvent {
    private MonitorTriggerActionInventory inventory;

    public APIUpdateMonitorTriggerActionEvent() {
    }

    public APIUpdateMonitorTriggerActionEvent(String apiId) {
        super(apiId);
    }

    public MonitorTriggerActionInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTriggerActionInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateMonitorTriggerActionEvent __example__() {
        APIUpdateMonitorTriggerActionEvent evt = new APIUpdateMonitorTriggerActionEvent();

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
