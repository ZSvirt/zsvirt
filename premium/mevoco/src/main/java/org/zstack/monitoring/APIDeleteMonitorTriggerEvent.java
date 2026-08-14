package org.zstack.monitoring;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/9.
 */
@RestResponse
public class APIDeleteMonitorTriggerEvent extends APIEvent {
    public APIDeleteMonitorTriggerEvent() {
    }

    public APIDeleteMonitorTriggerEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteMonitorTriggerEvent __example__() {
        APIDeleteMonitorTriggerEvent evt = new APIDeleteMonitorTriggerEvent();
        return evt;
    }
}
