package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent extends APIEvent {
    public APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent() {
        super(null);
    }
}
