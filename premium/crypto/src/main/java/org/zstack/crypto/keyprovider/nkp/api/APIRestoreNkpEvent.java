package org.zstack.crypto.keyprovider.nkp.api;

import org.zstack.header.keyprovider.NkpInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIRestoreNkpEvent extends APIEvent {
    private NkpInventory inventory;

    public APIRestoreNkpEvent() {
        super(null);
    }

    public APIRestoreNkpEvent(String apiId) {
        super(apiId);
    }

    public NkpInventory getInventory() {
        return inventory;
    }

    public void setInventory(NkpInventory inventory) {
        this.inventory = inventory;
    }

    public static APIRestoreNkpEvent __example__() {
        APIRestoreNkpEvent event = new APIRestoreNkpEvent();
        event.setInventory(NkpInventory.__example__());
        return event;
    }
}
