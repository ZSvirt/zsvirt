package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddLabelToAlarmEvent extends APIEvent {
    private AlarmLabelInventory inventory;

    public static APIAddLabelToAlarmEvent __example__() {
        APIAddLabelToAlarmEvent ret = new APIAddLabelToAlarmEvent();
        ret.inventory = AlarmLabelInventory.__example__();
        return ret;
    }

    public APIAddLabelToAlarmEvent() {
    }

    public APIAddLabelToAlarmEvent(String apiId) {
        super(apiId);
    }

    public AlarmLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmLabelInventory inventory) {
        this.inventory = inventory;
    }
}
