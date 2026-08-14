package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api remove dns from l3Network
 * @category l3network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APIRemoveDnsFromL3NetworkMsg": {
 * "l3NetworkUuid": "f14fd6ff593a41dd8c6caafc1f5448f9",
 * "dns": "8.8.8.8",
 * "session": {
 * "uuid": "db6ab57c757b403f898872adc01ead9e"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APIRemoveDnsFromL3NetworkMsg": {
 * "l3NetworkUuid": "f14fd6ff593a41dd8c6caafc1f5448f9",
 * "dns": "8.8.8.8",
 * "session": {
 * "uuid": "db6ab57c757b403f898872adc01ead9e"
 * },
 * "timeout": 1800000,
 * "id": "515bf56f9bcc46e2826e0af653dffe64",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIRemoveDnsFromL3NetworkEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/dns/{dns}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveDnsFromL3NetworkEvent.class
)
public class APIRemoveDnsFromL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc dns ip address
     */
    @APIParam
    private String dns;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }
 
    public static APIRemoveDnsFromL3NetworkMsg __example__() {
        APIRemoveDnsFromL3NetworkMsg msg = new APIRemoveDnsFromL3NetworkMsg();

        msg.setL3NetworkUuid(uuid());
        msg.setDns("8.8.4.4");

        return msg;
    }
}
