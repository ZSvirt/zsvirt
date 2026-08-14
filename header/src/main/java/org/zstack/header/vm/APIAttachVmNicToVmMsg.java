package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/nices/{vmNicUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachVmNicToVmEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIAttachVmNicToVmMsg extends APIMessage implements VmInstanceMessage {

    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIAttachVmNicToVmMsg __example__() {
        APIAttachVmNicToVmMsg msg = new APIAttachVmNicToVmMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setVmNicUuid(uuid());
        return msg;
    }
}
