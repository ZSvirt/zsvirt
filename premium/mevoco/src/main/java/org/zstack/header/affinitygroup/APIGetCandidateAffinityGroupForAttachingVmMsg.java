package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.header.affinitygroup
 * @date 2021/1/12 1:19 PM
 */
@RestRequest(
        path = "/affinityGroup/attachingVm",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateAffinityGroupForAttachingVmReply.class
)
public class APIGetCandidateAffinityGroupForAttachingVmMsg extends APISyncCallMessage {

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public static APIGetCandidateAffinityGroupForAttachingVmMsg __example__() {
        APIGetCandidateAffinityGroupForAttachingVmMsg msg = new APIGetCandidateAffinityGroupForAttachingVmMsg();
        msg.setVmUuid(uuid());
        return msg;
    }
}
