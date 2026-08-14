package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/xml-hook-script",
        method = HttpMethod.GET,
        responseClass = APIGetVmXmlHookScriptReply.class
)
public class APIGetVmXmlHookScriptMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public static APIGetVmXmlHookScriptMsg __example__() {
        APIGetVmXmlHookScriptMsg msg = new APIGetVmXmlHookScriptMsg();
        msg.setVmInstanceUuid(uuid());

        return msg;
    }
}
