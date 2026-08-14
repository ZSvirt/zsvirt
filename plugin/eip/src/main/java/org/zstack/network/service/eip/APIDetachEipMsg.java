package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

import java.util.ArrayList;
import java.util.List;

/**
 * @api
 *
 * detach eip from vm nic
 *
 * @category eip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *{
"org.zstack.network.service.eip.APIDetachEipMsg": {
"uuid": "69198105fd7a40778fba1759b923545c",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
}
}
}
 * @msg
 * {
"org.zstack.network.service.eip.APIDetachEipMsg": {
"uuid": "69198105fd7a40778fba1759b923545c",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
},
"timeout": 1800000,
"id": "2bbc35d895b445da8b1a869f9b351458",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDetachEipEvent`
 */
@RestRequest(
        path = "/eips/{uuid}/vm-instances/nics",
        method = HttpMethod.DELETE,
        responseClass = APIDetachEipEvent.class
)
public class APIDetachEipMsg extends APIMessage implements EipMessage, APIMultiAuditor {
    /**
     * @desc eip uuid
     */
    @APIParam(resourceType = EipVO.class)
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

    @Override
    public String getEipUuid() {
        return uuid;
    }
 
    public static APIDetachEipMsg __example__() {
        APIDetachEipMsg msg = new APIDetachEipMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APIDetachEipMsg amsg = (APIDetachEipMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        res.add(new APIAuditor.Result(amsg.getUuid(), EipVO.class));
        res.add(new APIAuditor.Result(amsg.vmNicUuid, VmNicVO.class));

        return res;
    }
}
