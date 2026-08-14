package org.zstack.network.service.flat;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

/**
 * Created by miao on 16-7-19.
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/dhcp-ip",
        method = HttpMethod.GET,
        responseClass = APIGetL3NetworkDhcpIpAddressReply.class
)
public class APIGetL3NetworkDhcpIpAddressMsg extends APISyncCallMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
 
    public static APIGetL3NetworkDhcpIpAddressMsg __example__() {
        APIGetL3NetworkDhcpIpAddressMsg msg = new APIGetL3NetworkDhcpIpAddressMsg();

        msg.setL3NetworkUuid(uuid());

        return msg;
    }

}
