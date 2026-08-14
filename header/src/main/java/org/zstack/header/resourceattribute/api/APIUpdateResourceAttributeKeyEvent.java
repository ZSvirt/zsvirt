package org.zstack.header.resourceattribute.api;

import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateResourceAttributeKeyEvent extends APIEvent {
    private ResourceAttributeKeyInventory inventory;

    public APIUpdateResourceAttributeKeyEvent() {
    }

    public APIUpdateResourceAttributeKeyEvent(String apiId) {
        super(apiId);
    }

    public ResourceAttributeKeyInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceAttributeKeyInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateResourceAttributeKeyEvent __example__() {
        APIUpdateResourceAttributeKeyEvent event = new APIUpdateResourceAttributeKeyEvent();
        event.setInventory(ResourceAttributeKeyInventory.__example__());
        return event;
    }
}
