package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveSchedulerJobsFromSchedulerJobGroupEvent extends APIEvent{
    public APIRemoveSchedulerJobsFromSchedulerJobGroupEvent() {
        super(null);
    }

    public APIRemoveSchedulerJobsFromSchedulerJobGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveSchedulerJobsFromSchedulerJobGroupEvent __example__() {
        APIRemoveSchedulerJobsFromSchedulerJobGroupEvent evt = new APIRemoveSchedulerJobsFromSchedulerJobGroupEvent();
        return evt;
    }
}
