package org.zstack.zwatch.alarm.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSNSTextTemplateEvent extends APIEvent {
    private SNSTextTemplateInventory inventory;

    public static APICreateSNSTextTemplateEvent __example__() {
        APICreateSNSTextTemplateEvent ret = new APICreateSNSTextTemplateEvent();
        ret.inventory = SNSTextTemplateInventory.__example__();
        return ret;
    }

    public APICreateSNSTextTemplateEvent() {
    }

    public APICreateSNSTextTemplateEvent(String apiId) {
        super(apiId);
    }

    public SNSTextTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSTextTemplateInventory inventory) {
        this.inventory = inventory;
    }
}
