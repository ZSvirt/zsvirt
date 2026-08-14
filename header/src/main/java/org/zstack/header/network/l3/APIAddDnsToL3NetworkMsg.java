package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api add dns to L3Network
 * @category l3network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l3.APIAddDnsToL3NetworkMsg": {
 * "session": {
 * "uuid": "b80d10a710f540528a0488f70cc18871"
 * },
 * "l3NetworkUuid": "564b95ad36b546a5ae9608e269cf1ac3",
 * "dns": "8.8.8.8"
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l3.APIAddDnsToL3NetworkMsg": {
 * "session": {
 * "uuid": "b80d10a710f540528a0488f70cc18871"
 * },
 * "l3NetworkUuid": "564b95ad36b546a5ae9608e269cf1ac3",
 * "dns": "8.8.8.8",
 * "timeout": 1800000,
 * "id": "cffeac3ad46d4fffa4262ecc8aaaa699",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIAddDnsToL3NetworkEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/dns",
        method = HttpMethod.POST,
        responseClass = APIAddDnsToL3NetworkEvent.class,
        parameterName = "params"
)
public class APIAddDnsToL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    /**
     * @desc dns in IPv4
     */
    @APIParam
    private String dns;

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
 
    public static APIAddDnsToL3NetworkMsg __example__() {
        APIAddDnsToL3NetworkMsg msg = new APIAddDnsToL3NetworkMsg();
        msg.setL3NetworkUuid(uuid());
        msg.setDns("8.8.8.8");

        return msg;
    }
}
