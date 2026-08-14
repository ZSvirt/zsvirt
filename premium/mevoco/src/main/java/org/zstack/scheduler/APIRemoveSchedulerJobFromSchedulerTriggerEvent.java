package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by AlanJager on 2017/6/8.
 */

@RestResponse
public class APIRemoveSchedulerJobFromSchedulerTriggerEvent extends APIEvent {
    public APIRemoveSchedulerJobFromSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public APIRemoveSchedulerJobFromSchedulerTriggerEvent() {
        super(null);
    }
}
