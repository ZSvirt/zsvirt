package org.zstack.sns;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeSNSApplicationEndpointStateEvent extends APIEvent {
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private SNSApplicationEndpointInventory inventory;

    public static APIChangeSNSApplicationEndpointStateEvent __example__() {
        APIChangeSNSApplicationEndpointStateEvent evt = new APIChangeSNSApplicationEndpointStateEvent();
        evt.setInventory(SNSApplicationEndpointInventory.__example__());
        return evt;
    }
    public APIChangeSNSApplicationEndpointStateEvent() {
    }

    public APIChangeSNSApplicationEndpointStateEvent(String apiId) {
        super(apiId);
    }

    public SNSApplicationEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSApplicationEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
