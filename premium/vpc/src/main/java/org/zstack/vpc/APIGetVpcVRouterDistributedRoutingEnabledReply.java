package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 20/11/2017
 */
@RestResponse(allTo = "enabled")
public class APIGetVpcVRouterDistributedRoutingEnabledReply extends APIReply {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static APIGetVpcVRouterDistributedRoutingEnabledReply __example__() {
        APIGetVpcVRouterDistributedRoutingEnabledReply reply = new APIGetVpcVRouterDistributedRoutingEnabledReply();

        reply.setEnabled(true);
        return reply;
    }
}
