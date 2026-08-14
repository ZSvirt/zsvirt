package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteMetricDataHttpReceiverEvent extends APIEvent {

    public static APIDeleteMetricDataHttpReceiverEvent __example__() {
        APIDeleteMetricDataHttpReceiverEvent ret = new APIDeleteMetricDataHttpReceiverEvent();
        return ret;
    }

    public APIDeleteMetricDataHttpReceiverEvent() {
    }

    public APIDeleteMetricDataHttpReceiverEvent(String apiId) {
        super(apiId);
    }
}
