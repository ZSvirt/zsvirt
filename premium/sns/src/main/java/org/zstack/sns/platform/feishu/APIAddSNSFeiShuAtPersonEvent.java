package org.zstack.sns.platform.feishu;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddSNSFeiShuAtPersonEvent extends APIEvent {
    private SNSFeiShuAtPersonInventory inventory;

    public static APIAddSNSFeiShuAtPersonEvent __example__() {
        APIAddSNSFeiShuAtPersonEvent evt = new APIAddSNSFeiShuAtPersonEvent();
        evt.setInventory(SNSFeiShuAtPersonInventory.__example__());
        return evt;
    }

    public APIAddSNSFeiShuAtPersonEvent() {
    }

    public APIAddSNSFeiShuAtPersonEvent(String apiId) {
        super(apiId);
    }

    public SNSFeiShuAtPersonInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSFeiShuAtPersonInventory inventory) {
        this.inventory = inventory;
    }
}
