package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Qi Le on 2019-07-11
 */
@RestResponse(allTo = "inventory")
public class APIAddSNSSmsReceiverEvent extends APIEvent {
    private SNSSmsReceiverInventory inventory;

    public static APIAddSNSSmsReceiverEvent __example__() {
        APIAddSNSSmsReceiverEvent event = new APIAddSNSSmsReceiverEvent();
        event.setInventory(SNSSmsReceiverInventory.__example__());
        return event;
    }

    public APIAddSNSSmsReceiverEvent() {
    }

    public APIAddSNSSmsReceiverEvent(String apiId) {
        super(apiId);
    }

    public SNSSmsReceiverInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSSmsReceiverInventory inventory) {
        this.inventory = inventory;
    }
}
