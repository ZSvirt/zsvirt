package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

/**
 * @api
 * detach a rule from vm nic
 *
 * @category port forwarding
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.portforwarding.APIDetachPortForwardingRuleMsg": {
"uuid": "26679c3eb6694a5e8e3528e5f7afe6d1",
"session": {
"uuid": "cc8751da40054ff2ac9d99f074edb875"
}
}
}
 * @msg
 * {
"org.zstack.network.service.portforwarding.APIDetachPortForwardingRuleMsg": {
"uuid": "26679c3eb6694a5e8e3528e5f7afe6d1",
"session": {
"uuid": "cc8751da40054ff2ac9d99f074edb875"
},
"timeout": 1800000,
"id": "2562e4f0e1dd4bd6a68d402b2b4c824a",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDetachPortForwardingRuleEvent`
 */
@RestRequest(
        path = "/port-forwarding/{uuid}/vm-instances/nics",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPortForwardingRuleEvent.class
)
public class APIDetachPortForwardingRuleMsg extends APIMessage implements APIAuditor {
    /**
     * @desc rule uuid
     */
    @APIParam(resourceType = PortForwardingRuleVO.class)
    private String uuid;

    // for audit purpose only
    @APINoSee
    public String vmNicUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDetachPortForwardingRuleMsg __example__() {
        APIDetachPortForwardingRuleMsg msg = new APIDetachPortForwardingRuleMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDetachPortForwardingRuleMsg)msg).vmNicUuid, VmNicVO.class);
    }
}
