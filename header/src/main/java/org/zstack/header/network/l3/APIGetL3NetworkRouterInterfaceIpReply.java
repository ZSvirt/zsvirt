package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetL3NetworkRouterInterfaceIpReply extends APIReply {
    private String routerInterfaceIp;

    public String getRouterInterfaceIp() {
        return routerInterfaceIp;
    }

    public void setRouterInterfaceIp(String routerInterfaceIp) {
        this.routerInterfaceIp = routerInterfaceIp;
    }

    public static APIGetL3NetworkRouterInterfaceIpReply __example__() {
        APIGetL3NetworkRouterInterfaceIpReply reply = new APIGetL3NetworkRouterInterfaceIpReply();
        reply.setRouterInterfaceIp("192.168.0.2");

        return reply;
    }
}
