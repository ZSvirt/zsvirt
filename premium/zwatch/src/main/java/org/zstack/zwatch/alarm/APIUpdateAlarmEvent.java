package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateAlarmEvent extends APIEvent {
    private AlarmInventory inventory;

    public static APIUpdateAlarmEvent __example__() {
        APIUpdateAlarmEvent ret = new APIUpdateAlarmEvent();
        ret.inventory = AlarmInventory.__example__();
        return ret;
    }

    public APIUpdateAlarmEvent() {
    }

    public APIUpdateAlarmEvent(String apiId) {
        super(apiId);
    }

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
