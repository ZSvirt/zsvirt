package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by miao on 11/3/16.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIReimageVmInstanceEvent.class,
        category = "vmInstance"
)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIReimageVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
 
    public static APIReimageVmInstanceMsg __example__() {
        APIReimageVmInstanceMsg msg = new APIReimageVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }
}
