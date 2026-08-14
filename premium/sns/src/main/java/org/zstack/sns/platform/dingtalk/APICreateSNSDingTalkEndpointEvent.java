package org.zstack.sns.platform.dingtalk;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSNSDingTalkEndpointEvent extends APIEvent {
    private SNSDingTalkEndpointInventory inventory;

    public static APICreateSNSDingTalkEndpointEvent __example__() {
        APICreateSNSDingTalkEndpointEvent evt = new APICreateSNSDingTalkEndpointEvent();
        evt.setInventory(SNSDingTalkEndpointInventory.__example1__());
        return evt;
    }

    public APICreateSNSDingTalkEndpointEvent() {
    }

    public APICreateSNSDingTalkEndpointEvent(String apiId) {
        super(apiId);
    }

    public SNSDingTalkEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSDingTalkEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
