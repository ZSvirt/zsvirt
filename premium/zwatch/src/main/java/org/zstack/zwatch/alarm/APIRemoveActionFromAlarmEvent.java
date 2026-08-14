package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRemoveActionFromAlarmEvent extends APIEvent {
    private AlarmInventory inventory;

    public static APIRemoveActionFromAlarmEvent __example__() {
        APIRemoveActionFromAlarmEvent ret = new APIRemoveActionFromAlarmEvent();
        ret.inventory = AlarmInventory.__example__();
        return ret;
    }

    public APIRemoveActionFromAlarmEvent() {
    }

    public APIRemoveActionFromAlarmEvent(String apiId) {
        super(apiId);
    }

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
