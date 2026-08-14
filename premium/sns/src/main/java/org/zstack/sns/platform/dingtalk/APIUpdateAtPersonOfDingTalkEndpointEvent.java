package org.zstack.sns.platform.dingtalk;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.sns.platform.feishu.SNSFeiShuAtPersonInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateAtPersonOfDingTalkEndpointEvent extends APIEvent {
    private SNSDingTalkAtPersonInventory inventory;

    public SNSDingTalkAtPersonInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSDingTalkAtPersonInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateAtPersonOfDingTalkEndpointEvent() {}

    public APIUpdateAtPersonOfDingTalkEndpointEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateAtPersonOfDingTalkEndpointEvent __example__() {
        APIUpdateAtPersonOfDingTalkEndpointEvent evt = new APIUpdateAtPersonOfDingTalkEndpointEvent();
        evt.setInventory(SNSDingTalkAtPersonInventory.__example__());
        return evt;
    }
}
