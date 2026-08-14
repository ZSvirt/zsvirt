package org.zstack.sns.platform.wecom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.sns.platform.email.SNSEmailAddressInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateAtPersonOfWeComEndpointEvent extends APIEvent {
    private SNSWeComAtPersonInventory inventory;

    public SNSWeComAtPersonInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSWeComAtPersonInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAtPersonOfWeComEndpointEvent() {}

    public APIUpdateAtPersonOfWeComEndpointEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateAtPersonOfWeComEndpointEvent __example__() {
        APIUpdateAtPersonOfWeComEndpointEvent evt = new APIUpdateAtPersonOfWeComEndpointEvent();
        evt.setInventory(SNSWeComAtPersonInventory.__example__());
        return evt;
    }
}
