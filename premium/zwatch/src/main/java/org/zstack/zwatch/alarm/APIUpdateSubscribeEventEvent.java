package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.EmergencyLevel;

@RestResponse(allTo = "inventory")
public class APIUpdateSubscribeEventEvent extends APIEvent {
    private EventSubscriptionInventory inventory;

    public APIUpdateSubscribeEventEvent() {
    }

    public static APIUpdateSubscribeEventEvent __example__() {
        APIUpdateSubscribeEventEvent ret = new APIUpdateSubscribeEventEvent();
        ret.setInventory(EventSubscriptionInventory.__example__());
        ret.getInventory().setEmergencyLevel(EmergencyLevel.Emergent.name());
        return ret;
    }


    public APIUpdateSubscribeEventEvent(String apiId) {
        super(apiId);
    }

    public EventSubscriptionInventory getInventory() {
        return inventory;
    }

    public void setInventory(EventSubscriptionInventory inventory) {
        this.inventory = inventory;
    }
}
