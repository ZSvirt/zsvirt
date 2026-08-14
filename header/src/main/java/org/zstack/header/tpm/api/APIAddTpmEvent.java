package org.zstack.header.tpm.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tpm.entity.TpmInventory;

@RestResponse(allTo = "inventory")
public class APIAddTpmEvent extends APIEvent {
    private TpmInventory inventory;

    public APIAddTpmEvent() {
    }

    public APIAddTpmEvent(String apiId) {
        super(apiId);
    }

    public TpmInventory getInventory() {
        return inventory;
    }

    public void setInventory(TpmInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddTpmEvent __example__() {
        APIAddTpmEvent event = new APIAddTpmEvent();
        event.setInventory(TpmInventory.__example__());
        return event;
    }
}
