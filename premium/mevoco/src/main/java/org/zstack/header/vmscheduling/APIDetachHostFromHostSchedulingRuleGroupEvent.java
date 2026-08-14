package org.zstack.header.vmscheduling;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/2
 */
@RestResponse
public class APIDetachHostFromHostSchedulingRuleGroupEvent extends APIEvent {
    public APIDetachHostFromHostSchedulingRuleGroupEvent() {
        super(null);
    }

    public APIDetachHostFromHostSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachHostFromHostSchedulingRuleGroupEvent __example__() {
        APIDetachHostFromHostSchedulingRuleGroupEvent event = new APIDetachHostFromHostSchedulingRuleGroupEvent();
        return event;
    }
}
