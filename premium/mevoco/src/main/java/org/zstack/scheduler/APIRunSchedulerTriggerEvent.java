package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerTriggerVO;

/**
 * Created by MaJin on 2019/4/16.
 */
@RestResponse
public class APIRunSchedulerTriggerEvent extends APIEvent {
    public APIRunSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public APIRunSchedulerTriggerEvent() {
        super();
    }

    public static APIRunSchedulerTriggerEvent __example__() {
        return new APIRunSchedulerTriggerEvent(uuid(SchedulerTriggerVO.class));
    }
}
