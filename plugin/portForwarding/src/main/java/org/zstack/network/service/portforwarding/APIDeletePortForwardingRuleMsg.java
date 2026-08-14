package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * delete a port forwarding rule
 *
 * @category port forwarding
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.portforwarding.APIDeletePortForwardingRuleMsg": {
"uuid": "b2e2632e5094430691ed9a7152e6e489",
"deleteMode": "Permissive",
"session": {
"uuid": "83e02bfaaffd4b88a05749d2c0f9ddde"
}
}
}
 *
 * @msg
 *
 * {
"org.zstack.network.service.portforwarding.APIDeletePortForwardingRuleMsg": {
"uuid": "b2e2632e5094430691ed9a7152e6e489",
"deleteMode": "Permissive",
"session": {
"uuid": "83e02bfaaffd4b88a05749d2c0f9ddde"
},
"timeout": 1800000,
"id": "b5a2333e1c864d7391f2db08118c1bab",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDetachPortForwardingRuleEvent`
 */

@RestRequest(
        path = "/port-forwarding/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePortForwardingRuleEvent.class
)
public class APIDeletePortForwardingRuleMsg extends APIDeleteMessage {
    /**
     * @desc
     * rule uuid
     */
    @APIParam(resourceType = PortForwardingRuleVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeletePortForwardingRuleMsg __example__() {
        APIDeletePortForwardingRuleMsg msg = new APIDeletePortForwardingRuleMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
