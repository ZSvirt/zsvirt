package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/2
 */
@RestResponse
public class APIDeleteHostSchedulingRuleGroupEvent extends APIEvent {
    public APIDeleteHostSchedulingRuleGroupEvent() {
    }

    public APIDeleteHostSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteHostSchedulingRuleGroupEvent __example__() {
        APIDeleteHostSchedulingRuleGroupEvent event = new APIDeleteHostSchedulingRuleGroupEvent();
        event.setSuccess(true);
        return event;
    }
}

