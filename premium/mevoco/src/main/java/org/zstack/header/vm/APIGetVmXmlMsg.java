package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/xml",
        method = HttpMethod.GET,
        responseClass = APIGetVmXmlReply.class
)
public class APIGetVmXmlMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public static APIGetVmXmlMsg __example__() {
        APIGetVmXmlMsg msg = new APIGetVmXmlMsg();
        msg.setVmInstanceUuid(uuid());

        return msg;
    }
}
