package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/xml-hook-script",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmUserDefinedXmlHookScriptEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIDeleteVmUserDefinedXmlHookScriptMsg extends APIDeleteMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIDeleteVmUserDefinedXmlHookScriptMsg __example__() {
        APIDeleteVmUserDefinedXmlHookScriptMsg msg = new APIDeleteVmUserDefinedXmlHookScriptMsg();
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
