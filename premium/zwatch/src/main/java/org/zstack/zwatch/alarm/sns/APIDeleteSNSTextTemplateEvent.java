package org.zstack.zwatch.alarm.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteSNSTextTemplateEvent extends APIEvent {
    public static APIDeleteSNSTextTemplateEvent __example__() {
        APIDeleteSNSTextTemplateEvent ret = new APIDeleteSNSTextTemplateEvent();
        return ret;
    }

    public APIDeleteSNSTextTemplateEvent() {
    }

    public APIDeleteSNSTextTemplateEvent(String apiId) {
        super(apiId);
    }
}
