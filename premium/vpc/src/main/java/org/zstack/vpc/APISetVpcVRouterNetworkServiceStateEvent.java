package org.zstack.vpc;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * created by zhanyong.miao on 25/02/2019
 */
@RestResponse(allTo = "state")
public class APISetVpcVRouterNetworkServiceStateEvent extends APIEvent {
    private String state;
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public APISetVpcVRouterNetworkServiceStateEvent() {
    }

    public APISetVpcVRouterNetworkServiceStateEvent(String apiId) {
        super(apiId);
    }

    public static APISetVpcVRouterNetworkServiceStateEvent __example__() {
        APISetVpcVRouterNetworkServiceStateEvent reply = new APISetVpcVRouterNetworkServiceStateEvent();

        reply.setState("enable");
        return reply;
    }
}
