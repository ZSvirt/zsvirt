package org.zstack.sns.platform.http;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSNSHttpEndpointEvent extends APIEvent {
    private SNSHttpEndpointInventory inventory;

    public SNSHttpEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSHttpEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
