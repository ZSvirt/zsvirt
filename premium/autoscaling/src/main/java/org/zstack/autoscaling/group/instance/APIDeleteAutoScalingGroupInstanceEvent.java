package org.zstack.autoscaling.group.instance;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2018/10/9
 */
@RestResponse
public class APIDeleteAutoScalingGroupInstanceEvent extends APIEvent {
    public APIDeleteAutoScalingGroupInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAutoScalingGroupInstanceEvent() {
        super(null);
    }

    public static APIDeleteAutoScalingGroupInstanceEvent __example__() {
        APIDeleteAutoScalingGroupInstanceEvent evt = new APIDeleteAutoScalingGroupInstanceEvent();
        evt.setSuccess(true);

        return evt;
    }
}
