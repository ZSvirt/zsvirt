package org.zstack.sns.platform.feishu;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSNSFeiShuEndpointEvent extends APIEvent {
    private SNSFeiShuEndpointInventory inventory;

    public static APICreateSNSFeiShuEndpointEvent __example__() {
        APICreateSNSFeiShuEndpointEvent evt = new APICreateSNSFeiShuEndpointEvent();
        evt.setInventory(SNSFeiShuEndpointInventory.__example1__());
        return evt;
    }

    public APICreateSNSFeiShuEndpointEvent() {
    }

    public APICreateSNSFeiShuEndpointEvent(String apiId) {
        super(apiId);
    }

    public SNSFeiShuEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSFeiShuEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
