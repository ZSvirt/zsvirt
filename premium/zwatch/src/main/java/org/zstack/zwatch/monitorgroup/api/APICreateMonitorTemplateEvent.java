package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateInventory;

@RestResponse(allTo = "inventory")
public class APICreateMonitorTemplateEvent extends APIEvent {

    private MonitorTemplateInventory inventory;

    public static APICreateMonitorTemplateEvent __example__() {
        APICreateMonitorTemplateEvent ret = new APICreateMonitorTemplateEvent();
        ret.setInventory(MonitorTemplateInventory.__example__());
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
