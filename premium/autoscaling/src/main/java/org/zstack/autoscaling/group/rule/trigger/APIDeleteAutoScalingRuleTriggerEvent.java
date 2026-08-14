package org.zstack.autoscaling.group.rule.trigger;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2018/9/16
 */
@RestResponse
public class APIDeleteAutoScalingRuleTriggerEvent extends APIEvent {
    public APIDeleteAutoScalingRuleTriggerEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAutoScalingRuleTriggerEvent() {
        super(null);
    }

    public static APIDeleteAutoScalingRuleTriggerEvent __example__() {
        APIDeleteAutoScalingRuleTriggerEvent evt = new APIDeleteAutoScalingRuleTriggerEvent();
        evt.setSuccess(true);

        return evt;
    }
}
