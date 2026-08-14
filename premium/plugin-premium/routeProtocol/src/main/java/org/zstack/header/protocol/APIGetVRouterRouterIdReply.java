package org.zstack.header.protocol;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "routerId")
public class APIGetVRouterRouterIdReply extends APIReply {
    private String routerId;

    public APIGetVRouterRouterIdReply() {
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public static APIGetVRouterRouterIdReply __example__() {
        APIGetVRouterRouterIdReply reply = new APIGetVRouterRouterIdReply();
        reply.setRouterId("10.10.10.1");

        return reply;
    }

}
