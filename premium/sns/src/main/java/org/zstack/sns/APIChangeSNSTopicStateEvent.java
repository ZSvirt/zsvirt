package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeSNSTopicStateEvent extends APIEvent {
    private SNSTopicInventory inventory;

    public static APIChangeSNSTopicStateEvent __example__() {
        APIChangeSNSTopicStateEvent evt = new APIChangeSNSTopicStateEvent();
        evt.setInventory(SNSTopicInventory.__example__());
        return evt;
    }

    public APIChangeSNSTopicStateEvent() {
    }

    public APIChangeSNSTopicStateEvent(String apiId) {
        super(apiId);
    }

    public SNSTopicInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSTopicInventory inventory) {
        this.inventory = inventory;
    }
}
