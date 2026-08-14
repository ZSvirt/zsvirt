package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteMetricTemplateEvent extends APIEvent {

    public static APIDeleteMetricTemplateEvent __example__() {
        APIDeleteMetricTemplateEvent ret = new APIDeleteMetricTemplateEvent();
        return ret;
    }

    public APIDeleteMetricTemplateEvent() {
    }

    public APIDeleteMetricTemplateEvent(String apiId) {
        super(apiId);
    }
}
