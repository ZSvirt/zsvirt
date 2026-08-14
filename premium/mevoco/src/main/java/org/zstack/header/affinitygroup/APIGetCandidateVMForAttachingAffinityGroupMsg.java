package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.header.affinitygroup
 * @date 2021/1/12 2:23 PM
 */
@RestRequest(
        path = "/VM/attachingGroup",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateVMForAttachingAffinityGroupReply.class
)
public class APIGetCandidateVMForAttachingAffinityGroupMsg extends APISyncCallMessage implements AffinityGroupMessage {
    @APIParam(resourceType = AffinityGroupVO.class)
    private String affinityGroupUuid;

    @Override
    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }

    public void setAffinityGroupUuid(String affinityGroupUuid) {
        this.affinityGroupUuid = affinityGroupUuid;
    }

    public static APIGetCandidateVMForAttachingAffinityGroupMsg __example__() {
        APIGetCandidateVMForAttachingAffinityGroupMsg msg = new APIGetCandidateVMForAttachingAffinityGroupMsg();
        msg.setAffinityGroupUuid(uuid());
        return msg;
    }
}
