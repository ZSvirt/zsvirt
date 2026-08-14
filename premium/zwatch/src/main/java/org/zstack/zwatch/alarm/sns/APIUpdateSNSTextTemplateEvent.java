package org.zstack.zwatch.alarm.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateSNSTextTemplateEvent extends APIEvent {
    private SNSTextTemplateInventory inventory;

    public static APIUpdateSNSTextTemplateEvent __example__() {
        APIUpdateSNSTextTemplateEvent ret = new APIUpdateSNSTextTemplateEvent();
        ret.inventory = SNSTextTemplateInventory.__example__();
        return ret;
    }

    public APIUpdateSNSTextTemplateEvent() {
    }

    public APIUpdateSNSTextTemplateEvent(String apiId) {
        super(apiId);
    }

    public SNSTextTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSTextTemplateInventory inventory) {
        this.inventory = inventory;
    }
}
