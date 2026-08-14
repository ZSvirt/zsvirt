package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/9/21.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/iso-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateIsoForAttachingVmReply.class
)
public class APIGetCandidateIsoForAttachingVmMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIGetCandidateIsoForAttachingVmMsg __example__() {
        APIGetCandidateIsoForAttachingVmMsg msg = new APIGetCandidateIsoForAttachingVmMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

}
