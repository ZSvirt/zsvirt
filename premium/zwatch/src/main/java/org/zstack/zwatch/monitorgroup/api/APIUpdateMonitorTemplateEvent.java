package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateInventory;


@RestResponse(allTo = "inventory")
public class APIUpdateMonitorTemplateEvent extends APIEvent {
    private MonitorTemplateInventory inventory;

    public MonitorTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(MonitorTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateMonitorTemplateEvent() {
    }

    public APIUpdateMonitorTemplateEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateMonitorTemplateEvent __example__() {
        APIUpdateMonitorTemplateEvent event = new APIUpdateMonitorTemplateEvent();
        APIUpdateMonitorTemplateMsg msg = APIUpdateMonitorTemplateMsg.__example__();
        MonitorTemplateInventory inventory = APICreateMonitorTemplateEvent.__example__().getInventory();
        inventory.setUuid(msg.getUuid());
        inventory.setName(msg.getName());
        inventory.setDescription(msg.getDescription());
        event.setInventory(inventory);
        return event;
    }
}
