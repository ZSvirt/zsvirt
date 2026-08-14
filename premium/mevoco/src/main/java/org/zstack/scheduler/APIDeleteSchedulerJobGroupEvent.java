package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteSchedulerJobGroupEvent extends APIEvent{

    public APIDeleteSchedulerJobGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteSchedulerJobGroupEvent() {
        super(null);
    }
 
    public static APIDeleteSchedulerJobGroupEvent __example__() {
        APIDeleteSchedulerJobGroupEvent event = new APIDeleteSchedulerJobGroupEvent();
        event.setSuccess(true);
        return event;
    }

}
