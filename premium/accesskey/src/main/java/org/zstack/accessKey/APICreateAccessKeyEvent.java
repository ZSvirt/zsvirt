package org.zstack.accessKey;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateAccessKeyEvent extends APIEvent {
    private AccessKeyInventory inventory;

    public APICreateAccessKeyEvent(String apiId) {
        super(apiId);
    }

    public APICreateAccessKeyEvent() {
        super(null);
    }

    public AccessKeyInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccessKeyInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateAccessKeyEvent __example__() {
        APICreateAccessKeyEvent event = new APICreateAccessKeyEvent();
        event.setInventory(AccessKeyInventory.__example__());
        return event;
    }

}
