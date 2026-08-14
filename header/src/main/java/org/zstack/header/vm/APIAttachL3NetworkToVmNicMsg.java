package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/nics/{vmNicUuid}/l3-networks/{l3NetworkUuid}",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APIAttachL3NetworkToVmNicEvent.class
)
@Deprecated
public class APIAttachL3NetworkToVmNicMsg extends APIMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;
    /**
     * @desc uuid of L3Network
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    private String staticIp;

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
 
    public static APIAttachL3NetworkToVmNicMsg __example__() {
        APIAttachL3NetworkToVmNicMsg msg = new APIAttachL3NetworkToVmNicMsg();
        msg.vmNicUuid = uuid();
        msg.l3NetworkUuid = uuid();
        return msg;
    }
}
