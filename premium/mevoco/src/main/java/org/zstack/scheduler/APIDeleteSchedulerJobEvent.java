package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/15/16.
 */
@RestResponse
public class APIDeleteSchedulerJobEvent extends APIEvent{

    public APIDeleteSchedulerJobEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteSchedulerJobEvent() {
        super(null);
    }
 
    public static APIDeleteSchedulerJobEvent __example__() {
        APIDeleteSchedulerJobEvent event = new APIDeleteSchedulerJobEvent();
        event.setSuccess(true);
        return event;
    }

}
