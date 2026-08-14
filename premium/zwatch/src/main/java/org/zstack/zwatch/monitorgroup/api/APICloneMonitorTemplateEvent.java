package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateInventory;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICloneMonitorTemplateEvent extends APIEvent {

    private MonitorTemplateInventory inventory;

    public static APICloneMonitorTemplateEvent __example__() {
        APICloneMonitorTemplateEvent ret = new APICloneMonitorTemplateEvent();
        MonitorTemplateInventory inventory = new MonitorTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setName("clone-by-template");
        inventory.setDescription("desc");
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.setInventory(inventory);
        return ret;
    }

    public APICloneMonitorTemplateEvent() {
    }

    public MonitorTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APICloneMonitorTemplateEvent(String apiId) {
        super(apiId);
    }
}
