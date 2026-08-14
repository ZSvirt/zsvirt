package org.zstack.header.vmscheduling;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@RestResponse
public class APIAddVmToVmSchedulingRuleGroupEvent extends APIEvent {
    public APIAddVmToVmSchedulingRuleGroupEvent() {
    }

    public APIAddVmToVmSchedulingRuleGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIAddVmToVmSchedulingRuleGroupEvent __example__() {
        APIAddVmToVmSchedulingRuleGroupEvent event = new APIAddVmToVmSchedulingRuleGroupEvent();
        event.setSuccess(true);
        return event;
    }
}
