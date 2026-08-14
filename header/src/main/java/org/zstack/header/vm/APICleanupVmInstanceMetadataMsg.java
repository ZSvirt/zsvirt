package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/vm-instances/metadata/cleanup",
        method = HttpMethod.PUT,
        responseClass = APICleanupVmInstanceMetadataEvent.class,
        isAction = true
)
public class APICleanupVmInstanceMetadataMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class, nonempty = true)
    private List<String> vmUuids;

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }

    public static APICleanupVmInstanceMetadataMsg __example__() {
        APICleanupVmInstanceMetadataMsg msg = new APICleanupVmInstanceMetadataMsg();
        msg.vmUuids = java.util.Arrays.asList(uuid(), uuid());
        return msg;
    }
}
