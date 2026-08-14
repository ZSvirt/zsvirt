package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddActionToAlarmEvent extends APIEvent {
    private AlarmInventory inventory;

    public static APIAddActionToAlarmEvent __example__() {
        APIAddActionToAlarmEvent ret = new APIAddActionToAlarmEvent();
        ret.inventory = AlarmInventory.__example__();
        return ret;
    }

    public APIAddActionToAlarmEvent() {
    }

    public APIAddActionToAlarmEvent(String apiId) {
        super(apiId);
    }

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
