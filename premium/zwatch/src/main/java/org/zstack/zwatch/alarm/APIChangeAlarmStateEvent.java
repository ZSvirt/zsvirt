package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeAlarmStateEvent extends APIEvent {
    private AlarmInventory inventory;

    public static APIChangeAlarmStateEvent __example__() {
        APIChangeAlarmStateEvent ret = new APIChangeAlarmStateEvent();
        ret.inventory = AlarmInventory.__example__();
        return ret;
    }

    public APIChangeAlarmStateEvent() {
    }

    public APIChangeAlarmStateEvent(String apiId) {
        super(apiId);
    }

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
