package org.zstack.zwatch.alarm.activealarm.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIChangeActiveAlarmStateEvent extends APIEvent {

    public static APIChangeActiveAlarmStateEvent __example__() {
        APIChangeActiveAlarmStateEvent ret = new APIChangeActiveAlarmStateEvent();
        return ret;
    }

    public APIChangeActiveAlarmStateEvent() {
    }

    public APIChangeActiveAlarmStateEvent(String apiId) {
        super(apiId);
    }
}
