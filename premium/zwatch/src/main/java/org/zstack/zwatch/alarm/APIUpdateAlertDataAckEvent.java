package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2020/10/25
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAlertDataAckEvent extends APIEvent {
    private AlertDataAckInventory inventory;

    public APIUpdateAlertDataAckEvent() {
    }

    public static APIUpdateAlertDataAckEvent __example__() {
        APIUpdateAlertDataAckEvent event = new APIUpdateAlertDataAckEvent();
        APIUpdateAlertDataAckMsg msg = APIUpdateAlertDataAckMsg.__example__();
        AlertDataAckInventory inventory = APIAckAlertDataEvent.__example__().getInventory();
        inventory.setResumeAlert(msg.getResumeAlert());
        inventory.setAlertDataUuid(msg.getAlertDataUuid());
        return event;
    }

    public APIUpdateAlertDataAckEvent(String apiId) {
        super(apiId);
    }

    public AlertDataAckInventory getInventory() {
        return inventory;
    }

    public void setInventory(AlertDataAckInventory inventory) {
        this.inventory = inventory;
    }
}
