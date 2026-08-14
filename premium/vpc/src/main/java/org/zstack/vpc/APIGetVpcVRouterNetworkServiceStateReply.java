package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * created by zhanyong.miao on 25/02/2019
 */
@RestResponse(allTo = "state")
public class APIGetVpcVRouterNetworkServiceStateReply extends APIReply {
    private String state;
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public APIGetVpcVRouterNetworkServiceStateReply() {
    }

    public static APIGetVpcVRouterNetworkServiceStateReply __example__() {
        APIGetVpcVRouterNetworkServiceStateReply reply = new APIGetVpcVRouterNetworkServiceStateReply();

        reply.setState("enable");
        return reply;
    }
}

