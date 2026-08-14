package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateSNSTopicEvent extends APIEvent {
    private SNSTopicInventory inventory;

    public static APIUpdateSNSTopicEvent __example__() {
        APIUpdateSNSTopicEvent evt = new APIUpdateSNSTopicEvent();
        evt.setInventory(SNSTopicInventory.__example__());
        return evt;
    }

    public APIUpdateSNSTopicEvent() {
    }

    public APIUpdateSNSTopicEvent(String apiId) {
        super(apiId);
    }

    public SNSTopicInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSTopicInventory inventory) {
        this.inventory = inventory;
    }
}
