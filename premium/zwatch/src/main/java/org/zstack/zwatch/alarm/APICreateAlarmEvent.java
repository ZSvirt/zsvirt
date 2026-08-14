package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateAlarmEvent extends APIEvent {
    private AlarmInventory inventory;

    public static APICreateAlarmEvent __example__() {
        APICreateAlarmEvent ret = new APICreateAlarmEvent();
        ret.setInventory(AlarmInventory.__example__());
        return ret;
    }

    public APICreateAlarmEvent() {
    }

    public APICreateAlarmEvent(String apiId) {
        super(apiId);
    }

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
