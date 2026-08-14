package org.zstack.autoscaling.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse
public class APIDeleteAutoScalingGroupEvent extends APIEvent {
    public APIDeleteAutoScalingGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAutoScalingGroupEvent() {
        super(null);
    }

    public static APIDeleteAutoScalingGroupEvent __example__() {
        APIDeleteAutoScalingGroupEvent evt = new APIDeleteAutoScalingGroupEvent();
        evt.setSuccess(true);

        return evt;
    }
}
