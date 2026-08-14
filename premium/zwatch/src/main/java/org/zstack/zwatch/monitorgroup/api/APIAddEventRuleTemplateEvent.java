package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateInventory;
import org.zstack.zwatch.namespace.VmNamespace;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIAddEventRuleTemplateEvent extends APIEvent {

    private EventRuleTemplateInventory inventory;

    public static APIAddEventRuleTemplateEvent __example__() {
        APIAddEventRuleTemplateEvent ret = new APIAddEventRuleTemplateEvent();
        EventRuleTemplateInventory inventory = new EventRuleTemplateInventory();
        inventory.setUuid(uuid());
        inventory.setMonitorTemplateUuid(uuid());
        inventory.setName("VMStateChanged");
        inventory.setNamespace("ZStack/VM");
        inventory.setEventName(VmNamespace.VMStateChanged.getName());
        inventory.setEmergencyLevel(EmergencyLevel.Normal);
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.setInventory(inventory);
        return ret;
    }

    public APIAddEventRuleTemplateEvent() {
    }

    public EventRuleTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddEventRuleTemplateEvent(String apiId) {
        super(apiId);
    }
}
