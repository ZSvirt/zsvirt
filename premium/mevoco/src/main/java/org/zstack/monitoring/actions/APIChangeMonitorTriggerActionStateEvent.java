package org.zstack.monitoring.actions;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.monitoring.MonitorTriggerStateEvent;

import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestResponse(allTo = "inventory")
public class APIChangeMonitorTriggerActionStateEvent extends APIEvent {
    private MonitorTriggerActionInventory inventory;

    public APIChangeMonitorTriggerActionStateEvent() {
    }

    public APIChangeMonitorTriggerActionStateEvent(String apiId) {
        super(apiId);
    }

    public MonitorTriggerActionInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTriggerActionInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeMonitorTriggerActionStateEvent __example__() {
        APIChangeMonitorTriggerActionStateEvent evt = new APIChangeMonitorTriggerActionStateEvent();
        MonitorTriggerActionInventory inventory = new MonitorTriggerActionInventory();
        inventory.setUuid(uuid());
        inventory.setName("triggerAction");
        inventory.setCreateDate(new Timestamp(DocUtils.date));
        inventory.setLastOpDate(new Timestamp(DocUtils.date));
        inventory.setState(MonitorTriggerStateEvent.enable.name());

        evt.setInventory(inventory);
        return evt;
    }
}
