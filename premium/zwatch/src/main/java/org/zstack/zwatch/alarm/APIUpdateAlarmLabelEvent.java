package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateAlarmLabelEvent extends APIEvent {
    private AlarmLabelInventory inventory;

    public static APIUpdateAlarmLabelEvent __example__() {
        APIUpdateAlarmLabelEvent ret = new APIUpdateAlarmLabelEvent();
        ret.inventory = AlarmLabelInventory.__example__();
        return ret;
    }

    public APIUpdateAlarmLabelEvent() {
    }

    public APIUpdateAlarmLabelEvent(String apiId) {
        super(apiId);
    }

    public AlarmLabelInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmLabelInventory inventory) {
        this.inventory = inventory;
    }
}
