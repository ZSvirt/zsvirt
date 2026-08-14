package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIAckAlertDataEvent extends APIEvent {

    private AlertDataAckInventory inventory;

    public static APIAckAlertDataEvent __example__() {
        APIAckAlertDataEvent event = new APIAckAlertDataEvent();
        APIAckAlarmDataMsg msg = APIAckAlarmDataMsg.__example__();
        AlertDataAckInventory inventory = new AlertDataAckInventory();
        inventory.setAlertDataUuid(msg.getAlertDataUuid());
        inventory.setAlertType(msg.getDataType());
        inventory.setAckPeriod(msg.getAckPeriodSec().longValue());
        inventory.setResourceUuid(msg.getResourceUuid());
        inventory.setAckDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setResumeAlert(false);
        inventory.setOperatorAccountUuid(uuid());
        event.setInventory(inventory);
        return event;
    }

    public APIAckAlertDataEvent() {
    }

    public AlertDataAckInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlertDataAckInventory inventory) {
        this.inventory = inventory;
    }

    public APIAckAlertDataEvent(String apiId) {
        super(apiId);
    }
}
