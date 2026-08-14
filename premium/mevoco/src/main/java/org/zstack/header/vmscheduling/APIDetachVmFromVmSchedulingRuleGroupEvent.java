package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestResponse
public class APIDetachVmFromVmSchedulingRuleGroupEvent extends APIEvent {
    public APIDetachVmFromVmSchedulingRuleGroupEvent() {
    }

    public APIDetachVmFromVmSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }
    public static APIDetachVmFromVmSchedulingRuleGroupEvent __example__() {
        APIDetachVmFromVmSchedulingRuleGroupEvent event = new APIDetachVmFromVmSchedulingRuleGroupEvent();
        event.setSuccess(true);
        return event;
    }
}
