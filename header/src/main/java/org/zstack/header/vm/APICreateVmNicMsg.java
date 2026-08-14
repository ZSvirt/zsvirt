package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;


@TagResourceType(VmNicVO.class)
@RestRequest(
        path = "/nics",
        method = HttpMethod.POST,
        responseClass = APICreateVmNicEvent.class,
        parameterName = "params"
)
public class APICreateVmNicMsg extends APICreateMessage implements APIAuditor {

    /**
     * @desc uuid of L3Network where the nic will be created
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    @APIParam(required = false)
    private String ip;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public static APICreateVmNicMsg __example__() {
        APICreateVmNicMsg msg = new APICreateVmNicMsg();
        msg.setL3NetworkUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVmNicEvent)rsp).getInventory().getUuid() : "", VmNicVO.class);
    }
}
