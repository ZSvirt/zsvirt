package org.zstack.sns.platform.wecom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSNSWeComEndpointEvent extends APIEvent {
    private SNSWeComEndpointInventory inventory;

    public static APICreateSNSWeComEndpointEvent __example__() {
        APICreateSNSWeComEndpointEvent evt = new APICreateSNSWeComEndpointEvent();
        evt.setInventory(SNSWeComEndpointInventory.__example1__());
        return evt;
    }

    public APICreateSNSWeComEndpointEvent() {
    }

    public APICreateSNSWeComEndpointEvent(String apiId) {
        super(apiId);
    }

    public SNSWeComEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSWeComEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
