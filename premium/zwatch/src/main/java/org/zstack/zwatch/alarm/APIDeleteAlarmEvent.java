package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteAlarmEvent extends APIEvent {

    public static APIDeleteAlarmEvent __example__() {
        APIDeleteAlarmEvent ret = new APIDeleteAlarmEvent();
        return ret;
    }

    public APIDeleteAlarmEvent() {
    }

    public APIDeleteAlarmEvent(String apiId) {
        super(apiId);
    }
}
