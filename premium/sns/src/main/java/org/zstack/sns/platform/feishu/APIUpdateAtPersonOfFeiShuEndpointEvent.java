package org.zstack.sns.platform.feishu;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateAtPersonOfFeiShuEndpointEvent extends APIEvent {
    private SNSFeiShuAtPersonInventory inventory;

    public SNSFeiShuAtPersonInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSFeiShuAtPersonInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAtPersonOfFeiShuEndpointEvent() {}

    public APIUpdateAtPersonOfFeiShuEndpointEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateAtPersonOfFeiShuEndpointEvent __example__() {
        APIUpdateAtPersonOfFeiShuEndpointEvent evt = new APIUpdateAtPersonOfFeiShuEndpointEvent();
        evt.setInventory(SNSFeiShuAtPersonInventory.__example__());
        return evt;
    }
}
