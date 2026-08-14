package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author: kefeng.wang
 * @date: 2018-11-17
 **/
@RestResponse
public class APIDeleteMetricDataEvent extends APIEvent {
    public APIDeleteMetricDataEvent() {
    }

    public APIDeleteMetricDataEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteMetricDataEvent __example__() {
        APIDeleteMetricDataEvent event = new APIDeleteMetricDataEvent();
        return event;
    }
}
