package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateInventory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;
import org.zstack.zwatch.namespace.VmNamespace;

import java.sql.Timestamp;


@RestResponse(allTo = "inventory")
public class APIUpdateEventRuleTemplateEvent extends APIEvent {
    private EventRuleTemplateInventory inventory;

    public EventRuleTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateEventRuleTemplateEvent() {
    }

    public APIUpdateEventRuleTemplateEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateEventRuleTemplateEvent __example__() {
        APIUpdateEventRuleTemplateEvent event = new APIUpdateEventRuleTemplateEvent();
        APIUpdateEventRuleTemplateMsg msg = new APIUpdateEventRuleTemplateMsg();
        EventRuleTemplateInventory inv = new EventRuleTemplateInventory();
        inv.setUuid(uuid());
        inv.setName(msg.getName());
        inv.setMonitorTemplateUuid(uuid());
        inv.setNamespace("ZStack/VM");
        inv.setEventName(VmNamespace.VMStateChanged.getName());
        inv.setEmergencyLevel(EmergencyLevel.Normal);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
