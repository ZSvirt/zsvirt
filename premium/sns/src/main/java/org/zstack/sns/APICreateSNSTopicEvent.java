package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.sns.SNSTopicInventory;

@RestResponse(allTo = "inventory")
public class APICreateSNSTopicEvent extends APIEvent {
    private SNSTopicInventory inventory;

    public static APICreateSNSTopicEvent __example__() {
        APICreateSNSTopicEvent evt = new APICreateSNSTopicEvent();
        evt.setInventory(SNSTopicInventory.__example__());
        return evt;
    }

    public SNSTopicInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSTopicInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateSNSTopicEvent() {
    }

    public APICreateSNSTopicEvent(String apiId) {
        super(apiId);
    }
}
