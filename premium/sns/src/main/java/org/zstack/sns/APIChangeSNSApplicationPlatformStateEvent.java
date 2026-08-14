package org.zstack.sns;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeSNSApplicationPlatformStateEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private SNSApplicationPlatformInventory inventory;

    public static APIChangeSNSApplicationPlatformStateEvent __example__() {
        APIChangeSNSApplicationPlatformStateEvent evt = new APIChangeSNSApplicationPlatformStateEvent();
        evt.setInventory(SNSApplicationPlatformInventory.__example__());
        return evt;
    }

    public APIChangeSNSApplicationPlatformStateEvent() {
    }

    public APIChangeSNSApplicationPlatformStateEvent(String apiId) {
        super(apiId);
    }

    public SNSApplicationPlatformInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSApplicationPlatformInventory inventory) {
        this.inventory = inventory;
    }
}
