package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/l2-networks/{l2NetworkUuid}/cluster-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateClustersForAttachingL2NetworkReply.class
)
public class APIGetCandidateClustersForAttachingL2NetworkMsg extends APIGetMessage {
    @APIParam(resourceType = L2NetworkVO.class)
    private String l2NetworkUuid;

    @APIParam(required = false)
    private List<String> clusterTypes;

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public List<String> getClusterTypes() {
        return clusterTypes;
    }

    public void setClusterTypes(List<String> clusterTypes) {
        this.clusterTypes = clusterTypes;
    }

    public static APIGetCandidateClustersForAttachingL2NetworkMsg __example__() {
        APIGetCandidateClustersForAttachingL2NetworkMsg msg = new APIGetCandidateClustersForAttachingL2NetworkMsg();
        msg.setL2NetworkUuid(uuid());
        return msg;
    }
}
