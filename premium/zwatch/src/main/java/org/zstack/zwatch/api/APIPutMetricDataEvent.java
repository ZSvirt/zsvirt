package org.zstack.zwatch.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIPutMetricDataEvent extends APIEvent {
    public APIPutMetricDataEvent() {
    }

    public APIPutMetricDataEvent(String apiId) {
        super(apiId);
    }

    public static APIPutMetricDataEvent __example__() {
        APIPutMetricDataEvent ret = new APIPutMetricDataEvent();
        return ret;
    }
}
