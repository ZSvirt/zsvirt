package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.*;

@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/candidate-eips",
        method = HttpMethod.GET,
        responseClass = APIGetVmNicAttachableEipsReply.class
)
public class APIGetVmNicAttachableEipsMsg extends APIGetMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    @APIParam(required = false, validValues = {"4", "6"})
    private Integer ipVersion;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public static APIGetVmNicAttachableEipsMsg __example__() {
        APIGetVmNicAttachableEipsMsg msg = new APIGetVmNicAttachableEipsMsg();
        msg.vmNicUuid = uuid();
        return msg;
    }
}
