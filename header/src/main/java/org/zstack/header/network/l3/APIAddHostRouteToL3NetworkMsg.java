package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/hostroute",
        method = HttpMethod.POST,
        responseClass = APIAddHostRouteToL3NetworkEvent.class,
        parameterName = "params"
)
public class APIAddHostRouteToL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc networkcidr in IPv4
     */
    @APIParam
    private String prefix;

    /**
     * @desc nexthop in IPv4
     */
    @APIParam
    private String nexthop;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNexthop() {
        return nexthop;
    }

    public void setNexthop(String nexthop) {
        this.nexthop = nexthop;
    }

    public static APIAddHostRouteToL3NetworkMsg __example__() {
        APIAddHostRouteToL3NetworkMsg msg = new APIAddHostRouteToL3NetworkMsg();
        msg.setL3NetworkUuid(uuid());
        msg.setPrefix("169.254.169.254/32");
        msg.setNexthop("192.168.1.254");

        return msg;
    }
}
