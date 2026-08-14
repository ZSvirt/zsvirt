package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestResponse
public class APIDeleteSchedulerTriggerEvent extends APIEvent {
    public APIDeleteSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteSchedulerTriggerEvent() {
        super(null);
    }

    public static APIDeleteSchedulerTriggerEvent __example__() {
        APIDeleteSchedulerTriggerEvent event = new APIDeleteSchedulerTriggerEvent();
        return event;
    }
}
