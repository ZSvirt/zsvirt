package org.zstack.sns;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateSNSApplicationPlatformEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private SNSApplicationPlatformInventory inventory;

    public static APIUpdateSNSApplicationPlatformEvent __example__() {
        APIUpdateSNSApplicationPlatformEvent evt = new APIUpdateSNSApplicationPlatformEvent();
        evt.setInventory(SNSApplicationPlatformInventory.__example__());
        return evt;
    }

    public APIUpdateSNSApplicationPlatformEvent() {
    }

    public APIUpdateSNSApplicationPlatformEvent(String apiId) {
        super(apiId);
    }

    public SNSApplicationPlatformInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSApplicationPlatformInventory inventory) {
        this.inventory = inventory;
    }
}
