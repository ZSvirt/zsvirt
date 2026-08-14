package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/convert-to-templatedVmInstance",
        method = HttpMethod.POST,
        responseClass = APIConvertVmInstanceToTemplatedVmInstanceEvent.class,
        parameterName = "params"
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIConvertVmInstanceToTemplatedVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIConvertVmInstanceToTemplatedVmInstanceMsg __example__() {
        APIConvertVmInstanceToTemplatedVmInstanceMsg msg = new APIConvertVmInstanceToTemplatedVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIConvertVmInstanceToTemplatedVmInstanceEvent)rsp).getInventory().getUuid() : "", TemplatedVmInstanceVO.class);
    }
}
