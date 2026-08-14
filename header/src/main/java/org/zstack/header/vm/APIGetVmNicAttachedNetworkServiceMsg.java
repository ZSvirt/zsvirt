package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/attached-networkservices",
        method = HttpMethod.GET,
        responseClass = APIGetVmNicAttachedNetworkServiceReply.class
)
public class APIGetVmNicAttachedNetworkServiceMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public static APIGetVmNicAttachedNetworkServiceMsg __example__() {
        APIGetVmNicAttachedNetworkServiceMsg msg = new APIGetVmNicAttachedNetworkServiceMsg();
        msg.vmNicUuid = uuid();
        return msg;
    }
}
