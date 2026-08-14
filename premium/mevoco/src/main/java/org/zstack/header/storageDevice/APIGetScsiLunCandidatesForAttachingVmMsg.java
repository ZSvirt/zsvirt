package org.zstack.header.storageDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Create by weiwang at 2018/10/25
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/candidate-storage-devices",
        method = HttpMethod.GET,
        responseClass = APIGetScsiLunCandidatesForAttachingVmReply.class
)
public class APIGetScsiLunCandidatesForAttachingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIGetScsiLunCandidatesForAttachingVmMsg __example__() {
        APIGetScsiLunCandidatesForAttachingVmMsg msg = new APIGetScsiLunCandidatesForAttachingVmMsg();
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}