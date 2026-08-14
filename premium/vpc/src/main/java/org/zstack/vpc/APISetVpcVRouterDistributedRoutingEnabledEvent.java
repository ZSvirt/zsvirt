package org.zstack.vpc;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 20/11/2017
 */
@RestResponse(allTo = "enabled")
public class APISetVpcVRouterDistributedRoutingEnabledEvent extends APIEvent {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public APISetVpcVRouterDistributedRoutingEnabledEvent() {
    }

    public APISetVpcVRouterDistributedRoutingEnabledEvent(String apiId) {
        super(apiId);
    }

    public static APISetVpcVRouterDistributedRoutingEnabledEvent __example__() {
        APISetVpcVRouterDistributedRoutingEnabledEvent reply = new APISetVpcVRouterDistributedRoutingEnabledEvent();

        reply.setEnabled(true);
        return reply;
    }
}
