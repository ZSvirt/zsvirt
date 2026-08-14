package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/cluster/{clusterUuid}/l2-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateL2NetworksForAttachingClusterReply.class
)
public class APIGetCandidateL2NetworksForAttachingClusterMsg extends APIGetMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIGetCandidateL2NetworksForAttachingClusterMsg __example__() {
        APIGetCandidateL2NetworksForAttachingClusterMsg msg = new APIGetCandidateL2NetworksForAttachingClusterMsg();
        msg.clusterUuid = uuid();
        return msg;
    }
}
