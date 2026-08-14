package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/cluster/hosts-network-facts/{clusterUuid}",
        method = HttpMethod.GET,
        responseClass = APIGetClusterHostNetworkFactsReply.class
)
public class APIGetClusterHostNetworkFactsMsg extends APIGetMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIGetClusterHostNetworkFactsMsg __example__() {
        APIGetClusterHostNetworkFactsMsg msg = new APIGetClusterHostNetworkFactsMsg();
        msg.setClusterUuid(uuid());
        return msg;
    }
}