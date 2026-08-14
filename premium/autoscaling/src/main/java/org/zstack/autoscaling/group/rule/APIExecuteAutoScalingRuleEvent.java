package org.zstack.autoscaling.group.rule;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2019/8/19
 */
@RestResponse(fieldsTo = "all")
public class APIExecuteAutoScalingRuleEvent extends APIEvent {
    private String scalingActivityUuid;

    public String getScalingActivityUuid() {
        return scalingActivityUuid;
    }

    public void setScalingActivityUuid(String scalingActivityUuid) {
        this.scalingActivityUuid = scalingActivityUuid;
    }

    public APIExecuteAutoScalingRuleEvent(String apiId) {
        super(apiId);
    }

    public APIExecuteAutoScalingRuleEvent() {
        super(null);
    }

    public static APIExecuteAutoScalingRuleEvent __example__() {
        APIExecuteAutoScalingRuleEvent evt = new APIExecuteAutoScalingRuleEvent();
        evt.setScalingActivityUuid(uuid());
        return evt;
    }
}
