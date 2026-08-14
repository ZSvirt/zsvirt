package org.zstack.autoscaling.template;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by weiwang at 2018/8/16
 */
@RestResponse
public class APIDeleteAutoScalingTemplateEvent extends APIEvent {
    public APIDeleteAutoScalingTemplateEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAutoScalingTemplateEvent() {
        super(null);
    }

    public static APIDeleteAutoScalingTemplateEvent __example__() {
        APIDeleteAutoScalingTemplateEvent evt = new APIDeleteAutoScalingTemplateEvent();
        evt.setSuccess(true);

        return evt;
    }
}
