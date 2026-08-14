package org.zstack.sns;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateSNSApplicationEndpointEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private SNSApplicationEndpointInventory inventory;

    public static APIUpdateSNSApplicationEndpointEvent __example__() {
        APIUpdateSNSApplicationEndpointEvent evt = new APIUpdateSNSApplicationEndpointEvent();
        evt.setInventory(SNSApplicationEndpointInventory.__example__());
        return evt;
    }

    public APIUpdateSNSApplicationEndpointEvent() {
    }

    public APIUpdateSNSApplicationEndpointEvent(String apiId) {
        super(apiId);
    }

    public SNSApplicationEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSApplicationEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
