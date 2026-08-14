package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/2
 */
@RestResponse
public class APIAddHostToHostSchedulingRuleGroupEvent extends APIEvent {

    public APIAddHostToHostSchedulingRuleGroupEvent() {
        super(null);
    }

    public APIAddHostToHostSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIAddHostToHostSchedulingRuleGroupEvent __example__() {
        APIAddHostToHostSchedulingRuleGroupEvent event = new APIAddHostToHostSchedulingRuleGroupEvent();
        return event;
    }
}
