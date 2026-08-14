package org.zstack.monitoring;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/12.
 */
@RestResponse
public class APIDetachMonitorTriggerActionFromTriggerEvent extends APIEvent {
    public APIDetachMonitorTriggerActionFromTriggerEvent() {
    }

    public APIDetachMonitorTriggerActionFromTriggerEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachMonitorTriggerActionFromTriggerEvent __example__() {
        APIDetachMonitorTriggerActionFromTriggerEvent evt = new APIDetachMonitorTriggerActionFromTriggerEvent();
        return evt;
    }
}
