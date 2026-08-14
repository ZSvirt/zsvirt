package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRemoveLabelFromAlarmEvent extends APIEvent {
    private AlarmInventory inventory;

    public APIRemoveLabelFromAlarmEvent() {
    }

    public APIRemoveLabelFromAlarmEvent(String apiId) {
        super(apiId);
    }

    public AlarmInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlarmInventory inventory) {
        this.inventory = inventory;
    }
}
