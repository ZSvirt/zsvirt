package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Qi Le on 2019-07-15
 */
@RestResponse(allTo = "inventory")
public class APIUpdateAliyunSmsSNSTextTemplateEvent extends APIEvent {
    private AliyunSmsSNSTextTemplateInventory inventory;

    public static APIUpdateAliyunSmsSNSTextTemplateEvent __example__() {
        APIUpdateAliyunSmsSNSTextTemplateEvent event = new APIUpdateAliyunSmsSNSTextTemplateEvent();
        event.inventory = AliyunSmsSNSTextTemplateInventory.__example__();
        return event;
    }

    public APIUpdateAliyunSmsSNSTextTemplateEvent() {
    }

    public APIUpdateAliyunSmsSNSTextTemplateEvent(String apiId) {
        super(apiId);
    }

    public AliyunSmsSNSTextTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(AliyunSmsSNSTextTemplateInventory inventory) {
        this.inventory = inventory;
    }
}
