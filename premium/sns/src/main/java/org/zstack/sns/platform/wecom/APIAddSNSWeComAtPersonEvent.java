package org.zstack.sns.platform.wecom;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddSNSWeComAtPersonEvent extends APIEvent {
    private SNSWeComAtPersonInventory inventory;

    public static APIAddSNSWeComAtPersonEvent __example__() {
        APIAddSNSWeComAtPersonEvent evt = new APIAddSNSWeComAtPersonEvent();
        evt.setInventory(SNSWeComAtPersonInventory.__example__());
        return evt;
    }

    public APIAddSNSWeComAtPersonEvent() {
    }

    public APIAddSNSWeComAtPersonEvent(String apiId) {
        super(apiId);
    }

    public SNSWeComAtPersonInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSWeComAtPersonInventory inventory) {
        this.inventory = inventory;
    }
}
