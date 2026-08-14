package org.zstack.header.tpm.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tpm.entity.TpmInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateTpmEvent extends APIEvent {
    private TpmInventory inventory;

    public APIUpdateTpmEvent() {
    }

    public APIUpdateTpmEvent(String apiId) {
        super(apiId);
    }

    public TpmInventory getInventory() {
        return inventory;
    }

    public void setInventory(TpmInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateTpmEvent __example__() {
        APIUpdateTpmEvent event = new APIUpdateTpmEvent();
        event.setInventory(TpmInventory.__example__());
        return event;
    }
}
