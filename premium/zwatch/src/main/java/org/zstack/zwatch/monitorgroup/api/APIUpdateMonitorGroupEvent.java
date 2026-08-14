package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupState;

import java.sql.Timestamp;


@RestResponse(allTo = "inventory")
public class APIUpdateMonitorGroupEvent extends APIEvent {
    private MonitorGroupInventory inventory;

    public MonitorGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateMonitorGroupEvent() {
    }

    public APIUpdateMonitorGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateMonitorGroupEvent __example__() {
        APIUpdateMonitorGroupEvent event = new APIUpdateMonitorGroupEvent();
        APIUpdateMonitorGroupMsg msg = APIUpdateMonitorGroupMsg.__example__();
        MonitorGroupInventory inv = new MonitorGroupInventory();
        inv.setUuid(uuid());
        inv.setName(msg.getName());
        inv.setDescription(msg.getDescription());
        inv.setActions(JSONObjectUtil.toJsonString(msg.getActions()));
        inv.setState(MonitorGroupState.Enabled);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
