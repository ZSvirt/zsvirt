package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;

@RestResponse(allTo = "inventory")
public class APIAddThirdpartyPlatformEvent extends APIEvent {

    private ThirdpartyPlatformInventory inventory;

    public static APIAddThirdpartyPlatformEvent __example__() {
        APIAddThirdpartyPlatformEvent ret = new APIAddThirdpartyPlatformEvent();
        return ret;
    }

    public APIAddThirdpartyPlatformEvent() {
    }

    public ThirdpartyPlatformInventory getInventory() {
        return inventory;
    }

    public void setInventory(ThirdpartyPlatformInventory inventory) {
        this.inventory = inventory;
    }

    public APIAddThirdpartyPlatformEvent(String apiId) {
        super(apiId);
    }
}
