package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupTemplateRefInventory;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIApplyMonitorTemplateToMonitorGroupEvent extends APIEvent {

    private MonitorGroupTemplateRefInventory inventory;

    public static APIApplyMonitorTemplateToMonitorGroupEvent __example__() {
        APIApplyMonitorTemplateToMonitorGroupEvent ret = new APIApplyMonitorTemplateToMonitorGroupEvent();
        MonitorGroupTemplateRefInventory inventory = new MonitorGroupTemplateRefInventory();
        inventory.setGroupUuid(uuid());
        inventory.setTemplateUuid(uuid());
        inventory.setUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return ret;
    }

    public APIApplyMonitorTemplateToMonitorGroupEvent() {
    }

    public MonitorGroupTemplateRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorGroupTemplateRefInventory inventory) {
        this.inventory = inventory;
    }

    public APIApplyMonitorTemplateToMonitorGroupEvent(String apiId) {
        super(apiId);
    }
}
