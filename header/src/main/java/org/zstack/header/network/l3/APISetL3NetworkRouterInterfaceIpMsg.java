package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/router-interface-ip",
        method = HttpMethod.POST,
        responseClass = APISetL3NetworkRouterInterfaceIpEvent.class,
        parameterName = "params"
)
public class APISetL3NetworkRouterInterfaceIpMsg extends APIMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    @APIParam
    private String routerInterfaceIp;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getRouterInterfaceIp() {
        return routerInterfaceIp;
    }

    public void setRouterInterfaceIp(String routerInterfaceIp) {
        this.routerInterfaceIp = routerInterfaceIp;
    }

    public static APISetL3NetworkRouterInterfaceIpMsg __example__() {
        APISetL3NetworkRouterInterfaceIpMsg msg = new APISetL3NetworkRouterInterfaceIpMsg();
        msg.setL3NetworkUuid(uuid());
        msg.setRouterInterfaceIp("192.168.10.2");

        return msg;
    }
}
