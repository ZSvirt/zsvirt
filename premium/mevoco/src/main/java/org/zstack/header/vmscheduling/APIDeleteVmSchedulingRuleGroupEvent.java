package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestResponse
public class APIDeleteVmSchedulingRuleGroupEvent extends APIEvent {
    public APIDeleteVmSchedulingRuleGroupEvent() {
    }

    public APIDeleteVmSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVmSchedulingRuleGroupEvent __example__() {
        APIDeleteVmSchedulingRuleGroupEvent event = new APIDeleteVmSchedulingRuleGroupEvent();
        event.setSuccess(true);
        return event;
    }
}
