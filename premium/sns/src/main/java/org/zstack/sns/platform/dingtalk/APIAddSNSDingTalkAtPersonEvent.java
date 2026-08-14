package org.zstack.sns.platform.dingtalk;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddSNSDingTalkAtPersonEvent extends APIEvent {
    private SNSDingTalkAtPersonInventory inventory;

    public static APIAddSNSDingTalkAtPersonEvent __example__() {
        APIAddSNSDingTalkAtPersonEvent evt = new APIAddSNSDingTalkAtPersonEvent();
        evt.setInventory(SNSDingTalkAtPersonInventory.__example__());
        return evt;
    }

    public APIAddSNSDingTalkAtPersonEvent() {
    }

    public APIAddSNSDingTalkAtPersonEvent(String apiId) {
        super(apiId);
    }

    public SNSDingTalkAtPersonInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSDingTalkAtPersonInventory inventory) {
        this.inventory = inventory;
    }
}
