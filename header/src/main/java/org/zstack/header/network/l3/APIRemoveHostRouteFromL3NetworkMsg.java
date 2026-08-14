package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/hostroute",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveHostRouteFromL3NetworkEvent.class
)
public class APIRemoveHostRouteFromL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc ip prefix
     */
    @APIParam
    private String prefix;

    @Override
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

    public static APIRemoveHostRouteFromL3NetworkMsg __example__() {
        APIRemoveHostRouteFromL3NetworkMsg msg = new APIRemoveHostRouteFromL3NetworkMsg();

        msg.setL3NetworkUuid(uuid());
        msg.setPrefix("169.254.169.254/32");

        return msg;
    }
}
