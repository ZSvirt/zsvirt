package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/primary-storage/sharedblockgroup/sharedblock-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetSharedBlockCandidateReply.class
)
public class APIGetSharedBlockCandidateMsg extends APISyncCallMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIGetSharedBlockCandidateMsg __example__() {
        APIGetSharedBlockCandidateMsg msg = new APIGetSharedBlockCandidateMsg();
        msg.setClusterUuid(uuid());
        return msg;
    }

}
