package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateInventory;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateMonitorTemplateEvent extends APIEvent {

    private MonitorTemplateInventory inventory;

    public static APICreateMonitorTemplateEvent __example__() {
        APICreateMonitorTemplateEvent ret = new APICreateMonitorTemplateEvent();
        MonitorTemplateInventory inventory = new MonitorTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setName("vm-template");
        inventory.setDescription("desc");
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.setInventory(inventory);
        return ret;
    }

    public APICreateMonitorTemplateEvent() {
    }

    public MonitorTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateMonitorTemplateEvent(String apiId) {
        super(apiId);
    }
}
