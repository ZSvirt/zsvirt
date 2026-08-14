package org.zstack.autoscaling.group.rule;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse
public class APIDeleteAutoScalingRuleEvent extends APIEvent {
    public APIDeleteAutoScalingRuleEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAutoScalingRuleEvent() {
        super(null);
    }

    public static APIDeleteAutoScalingRuleEvent __example__() {
        APIDeleteAutoScalingRuleEvent evt = new APIDeleteAutoScalingRuleEvent();
        evt.setSuccess(true);

        return evt;
    }
}
