package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/router-interface-ip",
        method = HttpMethod.GET,
        responseClass = APIGetL3NetworkRouterInterfaceIpReply.class
)
public class APIGetL3NetworkRouterInterfaceIpMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public static APIGetL3NetworkMtuMsg __example__() {
        APIGetL3NetworkMtuMsg msg = new APIGetL3NetworkMtuMsg();
        msg.setL3NetworkUuid(uuid());

        return msg;
    }
}
