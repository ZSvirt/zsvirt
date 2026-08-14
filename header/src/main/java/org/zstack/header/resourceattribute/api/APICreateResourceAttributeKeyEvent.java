package org.zstack.header.resourceattribute.api;

import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateResourceAttributeKeyEvent extends APIEvent {
    private ResourceAttributeKeyInventory inventory;

    public APICreateResourceAttributeKeyEvent() {
    }

    public APICreateResourceAttributeKeyEvent(String apiId) {
        super(apiId);
    }

    public ResourceAttributeKeyInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceAttributeKeyInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateResourceAttributeKeyEvent __example__() {
        APICreateResourceAttributeKeyEvent event = new APICreateResourceAttributeKeyEvent();
        event.setInventory(ResourceAttributeKeyInventory.__example__());
        return event;
    }
}
